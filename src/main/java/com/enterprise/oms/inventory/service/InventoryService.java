package com.enterprise.oms.inventory.service;

import com.enterprise.oms.common.exception.ConcurrencyException;
import com.enterprise.oms.common.exception.InsufficientStockException;
import com.enterprise.oms.common.exception.ResourceNotFoundException;
import com.enterprise.oms.common.lock.DistributedLockService;
import com.enterprise.oms.inventory.domain.Inventory;
import com.enterprise.oms.inventory.domain.StockReservation;
import com.enterprise.oms.inventory.domain.StockReservation.ReservationStatus;
import com.enterprise.oms.inventory.domain.Warehouse;
import com.enterprise.oms.inventory.dto.InventoryResponse;
import com.enterprise.oms.inventory.dto.ReserveInventoryRequest;
import com.enterprise.oms.inventory.dto.StockUpdateRequest;
import com.enterprise.oms.inventory.repository.InventoryRepository;
import com.enterprise.oms.inventory.repository.StockReservationRepository;
import com.enterprise.oms.inventory.repository.WarehouseRepository;
import com.enterprise.oms.product.domain.Product;
import com.enterprise.oms.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;
    private final DistributedLockService distributedLockService;

    @Transactional
    public List<StockReservation> reserveInventory(ReserveInventoryRequest request) {
        log.info("Processing inventory reservation for Order ID: {}", request.getOrderId());
        List<StockReservation> reservations = new ArrayList<>();

        for (ReserveInventoryRequest.ItemToReserve item : request.getItems()) {
            String lockKey = "lock:inventory:product:" + item.getProductId();
            String lockValue = UUID.randomUUID().toString();

            boolean acquired = distributedLockService.acquireLock(lockKey, lockValue, 10);
            if (!acquired) {
                log.warn("Could not acquire lock for product ID: {} due to concurrent request", item.getProductId());
                throw new ConcurrencyException("Concurrent inventory lock failed for product: " + item.getProductId());
            }

            try {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + item.getProductId()));

                List<Inventory> inventoryList = inventoryRepository.findByProductId(item.getProductId());
                int totalAvailable = inventoryList.stream().mapToInt(Inventory::getQuantityAvailable).sum();

                if (totalAvailable < item.getQuantity()) {
                    throw new InsufficientStockException("Insufficient stock for product " + product.getName() +
                            ". Requested: " + item.getQuantity() + ", Available: " + totalAvailable);
                }

                int remainingToReserve = item.getQuantity();
                for (Inventory inv : inventoryList) {
                    if (remainingToReserve <= 0) break;

                    int alloc = Math.min(inv.getQuantityAvailable(), remainingToReserve);
                    if (alloc > 0) {
                        inv.setQuantityAvailable(inv.getQuantityAvailable() - alloc);
                        inv.setQuantityReserved(inv.getQuantityReserved() + alloc);
                        inventoryRepository.save(inv);
                        remainingToReserve -= alloc;
                    }
                }

                StockReservation reservation = StockReservation.builder()
                        .orderId(request.getOrderId())
                        .product(product)
                        .quantity(item.getQuantity())
                        .status(ReservationStatus.RESERVED)
                        .expiresAt(Instant.now().plus(15, ChronoUnit.MINUTES)) // 15 min TTL reservation
                        .build();

                reservations.add(stockReservationRepository.save(reservation));
            } finally {
                distributedLockService.releaseLock(lockKey, lockValue);
            }
        }

        log.info("Successfully reserved stock for Order ID: {}", request.getOrderId());
        return reservations;
    }

    @Transactional
    public void confirmReservation(UUID orderId) {
        log.info("Confirming inventory reservation permanently for Order ID: {}", orderId);
        List<StockReservation> reservations = stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        for (StockReservation res : reservations) {
            res.setStatus(ReservationStatus.CONFIRMED);
            stockReservationRepository.save(res);

            List<Inventory> inventoryList = inventoryRepository.findByProductId(res.getProduct().getId());
            int remaining = res.getQuantity();
            for (Inventory inv : inventoryList) {
                if (remaining <= 0) break;

                int dec = Math.min(inv.getQuantityReserved(), remaining);
                if (dec > 0) {
                    inv.setQuantityReserved(inv.getQuantityReserved() - dec);
                    inventoryRepository.save(inv);
                    remaining -= dec;
                }
            }
        }
    }

    @Transactional
    public void releaseReservation(UUID orderId) {
        log.info("Releasing inventory reservation for Order ID: {}", orderId);
        List<StockReservation> reservations = stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        for (StockReservation res : reservations) {
            res.setStatus(ReservationStatus.RELEASED);
            stockReservationRepository.save(res);

            List<Inventory> inventoryList = inventoryRepository.findByProductId(res.getProduct().getId());
            int remaining = res.getQuantity();
            for (Inventory inv : inventoryList) {
                if (remaining <= 0) break;

                int rel = Math.min(inv.getQuantityReserved(), remaining);
                if (rel > 0) {
                    inv.setQuantityReserved(inv.getQuantityReserved() - rel);
                    inv.setQuantityAvailable(inv.getQuantityAvailable() + rel);
                    inventoryRepository.save(inv);
                    remaining -= rel;
                }
            }
        }
    }

    @Scheduled(fixedRate = 60000) // Check every 60 seconds
    @Transactional
    public void releaseExpiredReservations() {
        List<StockReservation> expired = stockReservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.RESERVED, Instant.now());

        if (!expired.isEmpty()) {
            log.info("Cleaning up {} expired inventory reservations", expired.size());
            for (StockReservation res : expired) {
                releaseReservation(res.getOrderId());
                res.setStatus(ReservationStatus.EXPIRED);
                stockReservationRepository.save(res);
            }
        }
    }

    @Transactional
    public InventoryResponse updateStock(StockUpdateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> Inventory.builder()
                        .product(product)
                        .warehouse(warehouse)
                        .quantityAvailable(0)
                        .quantityReserved(0)
                        .build());

        inventory.setQuantityAvailable(request.getQuantityAvailable());
        inventory = inventoryRepository.save(inventory);

        return mapToResponse(inventory);
    }

    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<InventoryResponse> getLowStockInventory(int threshold) {
        return inventoryRepository.findLowStockInventory(threshold).stream().map(this::mapToResponse).toList();
    }

    public InventoryResponse mapToResponse(Inventory inv) {
        return InventoryResponse.builder()
                .id(inv.getId())
                .productId(inv.getProduct().getId())
                .productName(inv.getProduct().getName())
                .productSku(inv.getProduct().getSku())
                .warehouseId(inv.getWarehouse().getId())
                .warehouseName(inv.getWarehouse().getName())
                .quantityAvailable(inv.getQuantityAvailable())
                .quantityReserved(inv.getQuantityReserved())
                .build();
    }
}

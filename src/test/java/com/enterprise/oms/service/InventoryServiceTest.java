package com.enterprise.oms.service;

import com.enterprise.oms.common.exception.InsufficientStockException;
import com.enterprise.oms.common.lock.DistributedLockService;
import com.enterprise.oms.inventory.domain.Inventory;
import com.enterprise.oms.inventory.domain.StockReservation;
import com.enterprise.oms.inventory.domain.Warehouse;
import com.enterprise.oms.inventory.dto.ReserveInventoryRequest;
import com.enterprise.oms.inventory.repository.InventoryRepository;
import com.enterprise.oms.inventory.repository.StockReservationRepository;
import com.enterprise.oms.inventory.repository.WarehouseRepository;
import com.enterprise.oms.inventory.service.InventoryService;
import com.enterprise.oms.product.domain.Product;
import com.enterprise.oms.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockReservationRepository stockReservationRepository;
    @Mock
    private DistributedLockService distributedLockService;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;
    private Warehouse warehouse;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(UUID.randomUUID()).name("Exclusive Laptop").sku("SKU-LAP").price(new BigDecimal("1000")).build();
        warehouse = Warehouse.builder().id(UUID.randomUUID()).code("WH-1").name("Main Warehouse").build();
        inventory = Inventory.builder().id(UUID.randomUUID()).product(product).warehouse(warehouse).quantityAvailable(1).quantityReserved(0).build();
    }

    @Test
    @DisplayName("Should successfully reserve stock when inventory is available")
    void testReserveInventorySuccess() {
        UUID orderId = UUID.randomUUID();
        ReserveInventoryRequest request = ReserveInventoryRequest.builder()
                .orderId(orderId)
                .items(List.of(ReserveInventoryRequest.ItemToReserve.builder()
                        .productId(product.getId())
                        .quantity(1)
                        .build()))
                .build();

        when(distributedLockService.acquireLock(anyString(), anyString(), anyLong())).thenReturn(true);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(List.of(inventory));
        when(stockReservationRepository.save(any(StockReservation.class))).thenAnswer(i -> i.getArgument(0));

        List<StockReservation> reservations = inventoryService.reserveInventory(request);

        assertNotNull(reservations);
        assertEquals(1, reservations.size());
        assertEquals(0, inventory.getQuantityAvailable());
        assertEquals(1, inventory.getQuantityReserved());
        verify(distributedLockService, times(1)).releaseLock(anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when requested stock exceeds available stock")
    void testReserveInventoryStockUnavailable() {
        UUID orderId = UUID.randomUUID();
        ReserveInventoryRequest request = ReserveInventoryRequest.builder()
                .orderId(orderId)
                .items(List.of(ReserveInventoryRequest.ItemToReserve.builder()
                        .productId(product.getId())
                        .quantity(5) // Stock is 1
                        .build()))
                .build();

        when(distributedLockService.acquireLock(anyString(), anyString(), anyLong())).thenReturn(true);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(List.of(inventory));

        assertThrows(InsufficientStockException.class, () -> inventoryService.reserveInventory(request));
        verify(distributedLockService, times(1)).releaseLock(anyString(), anyString());
    }
}

package com.enterprise.oms.dashboard;

import com.enterprise.oms.inventory.dto.InventoryResponse;
import com.enterprise.oms.inventory.service.InventoryService;
import com.enterprise.oms.order.domain.OrderStatus;
import com.enterprise.oms.order.repository.OrderRepository;
import com.enterprise.oms.payment.domain.PaymentStatus;
import com.enterprise.oms.payment.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryService inventoryService;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardSummaryDto {
        private long totalOrders;
        private BigDecimal totalRevenue;
        private long pendingOrders;
        private long failedPayments;
        private List<InventoryResponse> lowStockProducts;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        long totalOrders = orderRepository.count();
        BigDecimal revenue = orderRepository.calculateTotalRevenue();
        if (revenue == null) revenue = BigDecimal.ZERO;

        long pendingOrders = orderRepository.countByStatus(OrderStatus.CREATED) +
                orderRepository.countByStatus(OrderStatus.INVENTORY_RESERVED) +
                orderRepository.countByStatus(OrderStatus.PAYMENT_PENDING);

        long failedPayments = paymentRepository.countByStatus(PaymentStatus.FAILED);
        List<InventoryResponse> lowStock = inventoryService.getLowStockInventory(10);

        return DashboardSummaryDto.builder()
                .totalOrders(totalOrders)
                .totalRevenue(revenue)
                .pendingOrders(pendingOrders)
                .failedPayments(failedPayments)
                .lowStockProducts(lowStock)
                .build();
    }
}

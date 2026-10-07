package com.enterprise.oms.order.repository;

import com.enterprise.oms.order.domain.Order;
import com.enterprise.oms.order.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByUserId(UUID userId, Pageable pageable);
    List<Order> findByUserId(UUID userId);
    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status IN (com.enterprise.oms.order.domain.OrderStatus.PAID, com.enterprise.oms.order.domain.OrderStatus.CONFIRMED, com.enterprise.oms.order.domain.OrderStatus.SHIPPED, com.enterprise.oms.order.domain.OrderStatus.OUT_FOR_DELIVERY, com.enterprise.oms.order.domain.OrderStatus.DELIVERED)")
    BigDecimal calculateTotalRevenue();
}

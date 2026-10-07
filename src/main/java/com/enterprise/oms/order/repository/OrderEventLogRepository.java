package com.enterprise.oms.order.repository;

import com.enterprise.oms.order.domain.OrderEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderEventLogRepository extends JpaRepository<OrderEventLog, UUID> {
    List<OrderEventLog> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}

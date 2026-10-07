package com.enterprise.oms.payment.repository;

import com.enterprise.oms.payment.domain.Payment;
import com.enterprise.oms.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByTransactionId(String transactionId);
    long countByStatus(PaymentStatus status);
}

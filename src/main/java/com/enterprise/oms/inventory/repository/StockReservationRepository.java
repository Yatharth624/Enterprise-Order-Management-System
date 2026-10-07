package com.enterprise.oms.inventory.repository;

import com.enterprise.oms.inventory.domain.StockReservation;
import com.enterprise.oms.inventory.domain.StockReservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {
    List<StockReservation> findByOrderId(UUID orderId);
    List<StockReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);
    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}

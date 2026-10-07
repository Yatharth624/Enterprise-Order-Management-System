package com.enterprise.oms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservationFailedEvent implements DomainEvent {
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    @Builder.Default
    private String eventType = "INVENTORY_RESERVATION_FAILED";
    @Builder.Default
    private Instant timestamp = Instant.now();

    private UUID orderId;
    private String reason;
}

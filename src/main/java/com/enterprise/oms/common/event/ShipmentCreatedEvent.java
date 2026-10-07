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
public class ShipmentCreatedEvent implements DomainEvent {
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    @Builder.Default
    private String eventType = "SHIPMENT_CREATED";
    @Builder.Default
    private Instant timestamp = Instant.now();

    private UUID shipmentId;
    private UUID orderId;
    private String trackingNumber;
    private String carrier;
}

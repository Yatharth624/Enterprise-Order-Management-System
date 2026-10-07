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
public class OrderCancelledEvent implements DomainEvent {
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    @Builder.Default
    private String eventType = "ORDER_CANCELLED";
    @Builder.Default
    private Instant timestamp = Instant.now();

    private UUID orderId;
    private UUID customerId;
    private String reason;
}

package com.enterprise.oms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements DomainEvent {
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    @Builder.Default
    private String eventType = "ORDER_CREATED";
    @Builder.Default
    private Instant timestamp = Instant.now();

    private UUID orderId;
    private String orderNumber;
    private UUID customerId;
    private BigDecimal totalAmount;
    private List<OrderItemEventPayload> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemEventPayload {
        private UUID productId;
        private String productName;
        private int quantity;
        private BigDecimal unitPrice;
    }
}

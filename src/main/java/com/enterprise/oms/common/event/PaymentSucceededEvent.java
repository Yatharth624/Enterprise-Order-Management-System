package com.enterprise.oms.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSucceededEvent implements DomainEvent {
    @Builder.Default
    private String eventId = UUID.randomUUID().toString();
    @Builder.Default
    private String eventType = "PAYMENT_SUCCEEDED";
    @Builder.Default
    private Instant timestamp = Instant.now();

    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private String transactionId;
}

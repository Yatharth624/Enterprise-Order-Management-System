package com.enterprise.oms.shipping.dto;

import com.enterprise.oms.shipping.domain.ShipmentStatus;
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
public class ShipmentResponse {
    private UUID id;
    private UUID orderId;
    private String trackingNumber;
    private String carrier;
    private ShipmentStatus status;
    private Instant estimatedDelivery;
    private Instant createdAt;
    private Instant updatedAt;
}

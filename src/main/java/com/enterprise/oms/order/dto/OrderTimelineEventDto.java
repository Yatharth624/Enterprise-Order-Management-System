package com.enterprise.oms.order.dto;

import com.enterprise.oms.order.domain.OrderStatus;
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
public class OrderTimelineEventDto {
    private UUID id;
    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private String reason;
    private Instant timestamp;
}

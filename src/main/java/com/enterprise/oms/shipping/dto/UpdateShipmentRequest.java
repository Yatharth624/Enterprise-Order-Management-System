package com.enterprise.oms.shipping.dto;

import com.enterprise.oms.shipping.domain.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShipmentRequest {
    @NotNull(message = "Shipment status is required")
    private ShipmentStatus status;
}

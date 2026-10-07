package com.enterprise.oms.shipping.controller;

import com.enterprise.oms.common.api.ApiResponse;
import com.enterprise.oms.common.api.CorrelationIdFilter;
import com.enterprise.oms.shipping.dto.ShipmentResponse;
import com.enterprise.oms.shipping.dto.UpdateShipmentRequest;
import com.enterprise.oms.shipping.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Endpoints for shipment tracking and status updates")
public class ShipmentController {

    private final ShippingService shippingService;

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get shipment details by Order ID")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipmentByOrderId(@PathVariable UUID orderId,
                                                                              HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        ShipmentResponse shipment = shippingService.getShipmentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(shipment, requestId));
    }

    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "Create shipment for an order (WAREHOUSE_MANAGER / ADMIN)")
    public ResponseEntity<ApiResponse<ShipmentResponse>> createShipment(@PathVariable UUID orderId,
                                                                        HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        ShipmentResponse shipment = shippingService.createShipment(orderId);
        return ResponseEntity.ok(ApiResponse.success(shipment, "Shipment created", requestId));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "Update shipment status (e.g. IN_TRANSIT, DELIVERED)")
    public ResponseEntity<ApiResponse<ShipmentResponse>> updateShipmentStatus(@PathVariable UUID id,
                                                                              @Valid @RequestBody UpdateShipmentRequest request,
                                                                              HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        ShipmentResponse shipment = shippingService.updateShipmentStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(shipment, "Shipment status updated", requestId));
    }
}

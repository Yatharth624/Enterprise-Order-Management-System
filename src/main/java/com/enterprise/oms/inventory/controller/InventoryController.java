package com.enterprise.oms.inventory.controller;

import com.enterprise.oms.common.api.ApiResponse;
import com.enterprise.oms.common.api.CorrelationIdFilter;
import com.enterprise.oms.inventory.dto.InventoryResponse;
import com.enterprise.oms.inventory.dto.ReserveInventoryRequest;
import com.enterprise.oms.inventory.dto.StockUpdateRequest;
import com.enterprise.oms.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Endpoints for stock management and inventory reservations")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "Get all inventory levels across warehouses")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventory(HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        List<InventoryResponse> inventory = inventoryService.getAllInventory();
        return ResponseEntity.ok(ApiResponse.success(inventory, requestId));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "Get low stock products below threshold")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold,
            HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        List<InventoryResponse> lowStock = inventoryService.getLowStockInventory(threshold);
        return ResponseEntity.ok(ApiResponse.success(lowStock, requestId));
    }

    @PostMapping("/update-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE_MANAGER')")
    @Operation(summary = "Update warehouse available stock")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateStock(@Valid @RequestBody StockUpdateRequest request,
                                                                      HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        InventoryResponse response = inventoryService.updateStock(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock updated successfully", requestId));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock for an order (with Redis distributed lock)")
    public ResponseEntity<ApiResponse<Void>> reserveInventory(@Valid @RequestBody ReserveInventoryRequest request,
                                                              HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        inventoryService.reserveInventory(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Inventory reserved successfully", requestId));
    }

    @PostMapping("/release/{orderId}")
    @Operation(summary = "Release reserved stock for an order")
    public ResponseEntity<ApiResponse<Void>> releaseInventory(@PathVariable UUID orderId,
                                                              HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        inventoryService.releaseReservation(orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Inventory released successfully", requestId));
    }
}

package com.enterprise.oms.order.controller;

import com.enterprise.oms.common.api.ApiResponse;
import com.enterprise.oms.common.api.CorrelationIdFilter;
import com.enterprise.oms.common.util.SecurityUtils;
import com.enterprise.oms.order.dto.CancelOrderRequest;
import com.enterprise.oms.order.dto.CreateOrderRequest;
import com.enterprise.oms.order.dto.OrderResponse;
import com.enterprise.oms.order.service.OrderService;
import com.enterprise.oms.payment.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Endpoints for order creation, tracking, and state management")
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    @Operation(summary = "Create a new order (Supports Idempotency-Key header)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest servletRequest) {

        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        String userEmail = SecurityUtils.getCurrentUserEmail();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            ApiResponse<OrderResponse> existingResponse = idempotencyService.checkIdempotency(idempotencyKey, OrderResponse.class);
            if (existingResponse != null) {
                return ResponseEntity.ok(existingResponse);
            }
        }

        OrderResponse order = orderService.createOrder(userEmail, request);
        ApiResponse<OrderResponse> response = ApiResponse.success(order, "Order created successfully", requestId);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.saveIdempotencyRecord(idempotencyKey, response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get list of orders (Customer gets own orders, ADMIN/STAFF gets all)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            HttpServletRequest servletRequest) {

        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        String userEmail = SecurityUtils.getCurrentUserEmail();

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest pageable = PageRequest.of(page, size, sort);

        Page<OrderResponse> orders;
        if (servletRequest.isUserInRole("ADMIN") || servletRequest.isUserInRole("SUPPORT_AGENT")) {
            orders = orderService.getAllOrders(pageable);
        } else {
            orders = orderService.getCustomerOrders(userEmail, pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(orders, requestId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed order information and visual timeline by Order ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable UUID id,
                                                                    HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        String userEmail = SecurityUtils.getCurrentUserEmail();
        OrderResponse order = orderService.getOrderById(id, userEmail);
        return ResponseEntity.ok(ApiResponse.success(order, requestId));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable UUID id,
                                                                   @RequestBody(required = false) CancelOrderRequest request,
                                                                   HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        String userEmail = SecurityUtils.getCurrentUserEmail();
        String reason = request != null ? request.getReason() : "User requested cancellation";
        OrderResponse order = orderService.cancelOrder(id, userEmail, reason);
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled successfully", requestId));
    }
}

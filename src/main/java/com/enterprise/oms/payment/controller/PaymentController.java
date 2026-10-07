package com.enterprise.oms.payment.controller;

import com.enterprise.oms.common.api.ApiResponse;
import com.enterprise.oms.common.api.CorrelationIdFilter;
import com.enterprise.oms.payment.dto.PaymentResponse;
import com.enterprise.oms.payment.dto.ProcessPaymentRequest;
import com.enterprise.oms.payment.dto.RetryPaymentRequest;
import com.enterprise.oms.payment.service.IdempotencyService;
import com.enterprise.oms.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints for payment processing, idempotency, and retries")
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    @Operation(summary = "Process payment for an order (Supports Idempotency-Key header)")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ProcessPaymentRequest request,
            HttpServletRequest servletRequest) {

        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            ApiResponse<PaymentResponse> existingResponse = idempotencyService.checkIdempotency(idempotencyKey, PaymentResponse.class);
            if (existingResponse != null) {
                return ResponseEntity.ok(existingResponse);
            }
        }

        PaymentResponse payment = paymentService.processPayment(request, idempotencyKey);
        ApiResponse<PaymentResponse> response = ApiResponse.success(payment, "Payment processed", requestId);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.saveIdempotencyRecord(idempotencyKey, response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment details by ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable UUID id,
                                                                        HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        PaymentResponse payment = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success(payment, requestId));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed payment (Supports Idempotency-Key header)")
    public ResponseEntity<ApiResponse<PaymentResponse>> retryPayment(
            @PathVariable UUID id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) RetryPaymentRequest request,
            HttpServletRequest servletRequest) {

        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            ApiResponse<PaymentResponse> existingResponse = idempotencyService.checkIdempotency(idempotencyKey, PaymentResponse.class);
            if (existingResponse != null) {
                return ResponseEntity.ok(existingResponse);
            }
        }

        PaymentResponse payment = paymentService.retryPayment(id, request);
        ApiResponse<PaymentResponse> response = ApiResponse.success(payment, "Payment retry processed", requestId);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.saveIdempotencyRecord(idempotencyKey, response);
        }

        return ResponseEntity.ok(response);
    }
}

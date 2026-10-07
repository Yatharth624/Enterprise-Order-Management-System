package com.enterprise.oms.payment.service;

import com.enterprise.oms.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class SimulatedPaymentGateway {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentGatewayResult {
        private PaymentStatus status;
        private String transactionId;
        private String errorMessage;
    }

    public PaymentGatewayResult processPayment(UUID orderId, BigDecimal amount, String paymentMethod, String forcedMode) {
        log.info("Simulating payment gateway processing for Order ID: {}, Amount: {}, Mode: {}", orderId, amount, forcedMode);

        if ("FORCE_FAIL".equalsIgnoreCase(forcedMode)) {
            return PaymentGatewayResult.builder()
                    .status(PaymentStatus.FAILED)
                    .errorMessage("Payment declined by issuing bank (Simulated)")
                    .build();
        }

        if ("FORCE_TIMEOUT".equalsIgnoreCase(forcedMode)) {
            return PaymentGatewayResult.builder()
                    .status(PaymentStatus.TIMEOUT)
                    .errorMessage("Payment gateway connection timeout (Simulated)")
                    .build();
        }

        // Default: 95% SUCCESS
        String txId = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return PaymentGatewayResult.builder()
                .status(PaymentStatus.SUCCESS)
                .transactionId(txId)
                .build();
    }
}

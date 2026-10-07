package com.enterprise.oms.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPaymentRequest {
    private String paymentMethod;
    private String simulateMode; // FORCE_SUCCESS, FORCE_FAIL
}

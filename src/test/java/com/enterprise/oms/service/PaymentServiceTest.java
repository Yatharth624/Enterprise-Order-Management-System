package com.enterprise.oms.service;

import com.enterprise.oms.messaging.producer.KafkaEventPublisher;
import com.enterprise.oms.payment.domain.Payment;
import com.enterprise.oms.payment.domain.PaymentStatus;
import com.enterprise.oms.payment.dto.PaymentResponse;
import com.enterprise.oms.payment.dto.ProcessPaymentRequest;
import com.enterprise.oms.payment.repository.PaymentRepository;
import com.enterprise.oms.payment.service.PaymentService;
import com.enterprise.oms.payment.service.SimulatedPaymentGateway;
import com.enterprise.oms.payment.service.SimulatedPaymentGateway.PaymentGatewayResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SimulatedPaymentGateway paymentGateway;
    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should successfully process payment and publish PaymentSucceededEvent")
    void testProcessPaymentSuccess() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("999.00"))
                .paymentMethod("CREDIT_CARD")
                .build();

        PaymentGatewayResult successResult = PaymentGatewayResult.builder()
                .status(PaymentStatus.SUCCESS)
                .transactionId("TXN-123456")
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });

        when(paymentGateway.processPayment(eq(orderId), any(BigDecimal.class), eq("CREDIT_CARD"), any())).thenReturn(successResult);

        PaymentResponse response = paymentService.processPayment(request, "IDEM-KEY-1");

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());
        assertEquals("TXN-123456", response.getTransactionId());
        verify(kafkaEventPublisher, times(1)).publishEvent(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle simulated payment failure and publish PaymentFailedEvent")
    void testProcessPaymentFailure() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .amount(new BigDecimal("999.00"))
                .paymentMethod("CREDIT_CARD")
                .simulateMode("FORCE_FAIL")
                .build();

        PaymentGatewayResult failResult = PaymentGatewayResult.builder()
                .status(PaymentStatus.FAILED)
                .errorMessage("Card declined")
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });

        when(paymentGateway.processPayment(eq(orderId), any(BigDecimal.class), eq("CREDIT_CARD"), eq("FORCE_FAIL"))).thenReturn(failResult);

        PaymentResponse response = paymentService.processPayment(request, "IDEM-KEY-2");

        assertNotNull(response);
        assertEquals(PaymentStatus.FAILED, response.getStatus());
        assertEquals("Card declined", response.getFailureReason());
        verify(kafkaEventPublisher, times(1)).publishEvent(anyString(), anyString(), any());
    }
}

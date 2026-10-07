package com.enterprise.oms.payment.service;

import com.enterprise.oms.common.config.KafkaConfig;
import com.enterprise.oms.common.event.PaymentFailedEvent;
import com.enterprise.oms.common.event.PaymentSucceededEvent;
import com.enterprise.oms.common.exception.PaymentFailedException;
import com.enterprise.oms.common.exception.ResourceNotFoundException;
import com.enterprise.oms.messaging.producer.KafkaEventPublisher;
import com.enterprise.oms.payment.domain.Payment;
import com.enterprise.oms.payment.domain.PaymentStatus;
import com.enterprise.oms.payment.dto.PaymentResponse;
import com.enterprise.oms.payment.dto.ProcessPaymentRequest;
import com.enterprise.oms.payment.dto.RetryPaymentRequest;
import com.enterprise.oms.payment.repository.PaymentRepository;
import com.enterprise.oms.payment.service.SimulatedPaymentGateway.PaymentGatewayResult;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SimulatedPaymentGateway paymentGateway;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Autowired(required = false)
    private Counter paymentFailuresCounter;

    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request, String idempotencyKey) {
        log.info("Initiating payment processing for Order ID: {}", request.getOrderId());

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        payment = paymentRepository.save(payment);

        PaymentGatewayResult gatewayResult = paymentGateway.processPayment(
                request.getOrderId(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getSimulateMode()
        );

        if (gatewayResult.getStatus() == PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(gatewayResult.getTransactionId());
            paymentRepository.save(payment);

            PaymentSucceededEvent successEvent = PaymentSucceededEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .amount(payment.getAmount())
                    .transactionId(gatewayResult.getTransactionId())
                    .build();

            kafkaEventPublisher.publishEvent(KafkaConfig.TOPIC_PAYMENT_EVENTS, payment.getId().toString(), successEvent);
            log.info("Payment SUCCESS for Order ID: {}, Transaction ID: {}", request.getOrderId(), gatewayResult.getTransactionId());
        } else {
            payment.setStatus(gatewayResult.getStatus());
            payment.setFailureReason(gatewayResult.getErrorMessage());
            paymentRepository.save(payment);

            if (paymentFailuresCounter != null) {
                paymentFailuresCounter.increment();
            }

            PaymentFailedEvent failEvent = PaymentFailedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .amount(payment.getAmount())
                    .failureReason(gatewayResult.getErrorMessage())
                    .build();

            kafkaEventPublisher.publishEvent(KafkaConfig.TOPIC_PAYMENT_EVENTS, payment.getId().toString(), failEvent);
            log.warn("Payment FAILED for Order ID: {}. Reason: {}", request.getOrderId(), gatewayResult.getErrorMessage());
        }

        return mapToResponse(payment);
    }

    @Transactional
    public PaymentResponse retryPayment(UUID paymentId, RetryPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return mapToResponse(payment);
        }

        String method = request != null && request.getPaymentMethod() != null ? request.getPaymentMethod() : payment.getPaymentMethod();
        String mode = request != null ? request.getSimulateMode() : null;

        PaymentGatewayResult gatewayResult = paymentGateway.processPayment(payment.getOrderId(), payment.getAmount(), method, mode);

        if (gatewayResult.getStatus() == PaymentStatus.SUCCESS) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(gatewayResult.getTransactionId());
            payment.setPaymentMethod(method);
            paymentRepository.save(payment);

            PaymentSucceededEvent successEvent = PaymentSucceededEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .amount(payment.getAmount())
                    .transactionId(gatewayResult.getTransactionId())
                    .build();

            kafkaEventPublisher.publishEvent(KafkaConfig.TOPIC_PAYMENT_EVENTS, payment.getId().toString(), successEvent);
        } else {
            payment.setStatus(gatewayResult.getStatus());
            payment.setFailureReason(gatewayResult.getErrorMessage());
            paymentRepository.save(payment);

            if (paymentFailuresCounter != null) {
                paymentFailuresCounter.increment();
            }

            PaymentFailedEvent failEvent = PaymentFailedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .amount(payment.getAmount())
                    .failureReason(gatewayResult.getErrorMessage())
                    .build();

            kafkaEventPublisher.publishEvent(KafkaConfig.TOPIC_PAYMENT_EVENTS, payment.getId().toString(), failEvent);
        }

        return mapToResponse(payment);
    }

    public PaymentResponse getPaymentById(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + id));
        return mapToResponse(payment);
    }

    public PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}

package com.enterprise.oms.messaging.consumer;

import com.enterprise.oms.common.config.KafkaConfig;
import com.enterprise.oms.common.event.PaymentFailedEvent;
import com.enterprise.oms.common.event.PaymentSucceededEvent;
import com.enterprise.oms.inventory.service.InventoryService;
import com.enterprise.oms.notification.service.NotificationService;
import com.enterprise.oms.order.domain.OrderStatus;
import com.enterprise.oms.order.dto.OrderResponse;
import com.enterprise.oms.order.service.OrderService;
import com.enterprise.oms.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventsConsumer {

    private final OrderService orderService;
    private final InventoryService inventoryService;
    private final ShippingService shippingService;
    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaConfig.TOPIC_PAYMENT_EVENTS, groupId = "oms-payment-group")
    public void handlePaymentEvent(Object eventPayload) {
        log.info("Received Kafka message in oms-payment-group: {}", eventPayload);

        if (eventPayload instanceof PaymentSucceededEvent event) {
            log.info("Processing PaymentSucceededEvent for Order ID: {}", event.getOrderId());
            try {
                // Transition order to PAID then CONFIRMED
                orderService.transitionOrderStatus(event.getOrderId(), OrderStatus.PAID, "Payment received. Txn ID: " + event.getTransactionId());
                orderService.transitionOrderStatus(event.getOrderId(), OrderStatus.CONFIRMED, "Order confirmed after payment success");

                // Confirm inventory
                inventoryService.confirmReservation(event.getOrderId());

                // Create shipment
                shippingService.createShipment(event.getOrderId());

                // Send notification
                OrderResponse order = orderService.getOrderById(event.getOrderId(), "system");
                notificationService.sendNotification(
                        order.getCustomerEmail(),
                        "Order Confirmed: #" + order.getOrderNumber(),
                        "Your payment of $" + event.getAmount() + " was successful. Your order is confirmed and currently being prepared for dispatch.",
                        "EMAIL"
                );
            } catch (Exception e) {
                log.error("Error processing PaymentSucceededEvent for order {}: {}", event.getOrderId(), e.getMessage());
            }
        } else if (eventPayload instanceof PaymentFailedEvent event) {
            log.warn("Processing PaymentFailedEvent for Order ID: {}", event.getOrderId());
            try {
                // Transition order to PAYMENT_FAILED
                orderService.transitionOrderStatus(event.getOrderId(), OrderStatus.PAYMENT_FAILED, "Payment failed: " + event.getFailureReason());

                // Release inventory stock
                inventoryService.releaseReservation(event.getOrderId());

                // Send failure notification
                OrderResponse order = orderService.getOrderById(event.getOrderId(), "system");
                notificationService.sendNotification(
                        order.getCustomerEmail(),
                        "Payment Failed: Order #" + order.getOrderNumber(),
                        "Your payment attempt failed due to: " + event.getFailureReason() + ". Please retry payment in your dashboard to proceed.",
                        "EMAIL"
                );
            } catch (Exception e) {
                log.error("Error processing PaymentFailedEvent for order {}: {}", event.getOrderId(), e.getMessage());
            }
        }
    }
}

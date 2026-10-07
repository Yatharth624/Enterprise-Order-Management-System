package com.enterprise.oms.messaging.consumer;

import com.enterprise.oms.common.config.KafkaConfig;
import com.enterprise.oms.common.event.ShipmentCreatedEvent;
import com.enterprise.oms.common.event.ShipmentDeliveredEvent;
import com.enterprise.oms.notification.service.NotificationService;
import com.enterprise.oms.order.dto.OrderResponse;
import com.enterprise.oms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingEventsConsumer {

    private final OrderService orderService;
    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaConfig.TOPIC_SHIPPING_EVENTS, groupId = "oms-shipping-group")
    public void handleShippingEvent(Object eventPayload) {
        log.info("Received Kafka message in oms-shipping-group: {}", eventPayload);

        if (eventPayload instanceof ShipmentCreatedEvent event) {
            log.info("Processing ShipmentCreatedEvent for Order ID: {}", event.getOrderId());
            try {
                OrderResponse order = orderService.getOrderById(event.getOrderId(), "system");
                notificationService.sendNotification(
                        order.getCustomerEmail(),
                        "Order Dispatched: #" + order.getOrderNumber(),
                        "Your order has been shipped via " + event.getCarrier() + ". Tracking Number: " + event.getTrackingNumber(),
                        "EMAIL"
                );
            } catch (Exception e) {
                log.error("Error sending shipment notification: {}", e.getMessage());
            }
        } else if (eventPayload instanceof ShipmentDeliveredEvent event) {
            log.info("Processing ShipmentDeliveredEvent for Order ID: {}", event.getOrderId());
            try {
                OrderResponse order = orderService.getOrderById(event.getOrderId(), "system");
                notificationService.sendNotification(
                        order.getCustomerEmail(),
                        "Order Delivered: #" + order.getOrderNumber(),
                        "Your order has been successfully delivered. Thank you for shopping with us!",
                        "EMAIL"
                );
            } catch (Exception e) {
                log.error("Error sending delivery notification: {}", e.getMessage());
            }
        }
    }
}

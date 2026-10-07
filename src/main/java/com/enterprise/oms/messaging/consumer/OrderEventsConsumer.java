package com.enterprise.oms.messaging.consumer;

import com.enterprise.oms.common.config.KafkaConfig;
import com.enterprise.oms.common.event.OrderCancelledEvent;
import com.enterprise.oms.common.event.OrderCreatedEvent;
import com.enterprise.oms.inventory.dto.ReserveInventoryRequest;
import com.enterprise.oms.inventory.service.InventoryService;
import com.enterprise.oms.order.domain.OrderStatus;
import com.enterprise.oms.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventsConsumer {

    private final InventoryService inventoryService;
    private final OrderService orderService;

    @KafkaListener(topics = KafkaConfig.TOPIC_ORDER_EVENTS, groupId = "oms-inventory-group")
    public void handleOrderEvent(Object eventPayload) {
        log.info("Received Kafka message in oms-inventory-group: {}", eventPayload);

        if (eventPayload instanceof OrderCreatedEvent event) {
            log.info("Processing OrderCreatedEvent for Order ID: {}", event.getOrderId());
            try {
                List<ReserveInventoryRequest.ItemToReserve> itemsToReserve = event.getItems().stream()
                        .map(item -> ReserveInventoryRequest.ItemToReserve.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList();

                ReserveInventoryRequest reserveRequest = ReserveInventoryRequest.builder()
                        .orderId(event.getOrderId())
                        .items(itemsToReserve)
                        .build();

                inventoryService.reserveInventory(reserveRequest);

                // Update state machine
                orderService.transitionOrderStatus(event.getOrderId(), OrderStatus.INVENTORY_RESERVED, "Stock reserved via Kafka event");
            } catch (Exception e) {
                log.error("Inventory reservation failed for order {}: {}", event.getOrderId(), e.getMessage());
                orderService.transitionOrderStatus(event.getOrderId(), OrderStatus.INVENTORY_FAILED, "Inventory reservation failed: " + e.getMessage());
            }
        } else if (eventPayload instanceof OrderCancelledEvent event) {
            log.info("Processing OrderCancelledEvent for Order ID: {}. Releasing inventory.", event.getOrderId());
            inventoryService.releaseReservation(event.getOrderId());
        }
    }
}

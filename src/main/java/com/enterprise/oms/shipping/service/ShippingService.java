package com.enterprise.oms.shipping.service;

import com.enterprise.oms.common.config.KafkaConfig;
import com.enterprise.oms.common.event.ShipmentCreatedEvent;
import com.enterprise.oms.common.event.ShipmentDeliveredEvent;
import com.enterprise.oms.common.exception.ResourceNotFoundException;
import com.enterprise.oms.messaging.producer.KafkaEventPublisher;
import com.enterprise.oms.order.domain.OrderStatus;
import com.enterprise.oms.order.service.OrderService;
import com.enterprise.oms.shipping.domain.Shipment;
import com.enterprise.oms.shipping.domain.ShipmentStatus;
import com.enterprise.oms.shipping.dto.ShipmentResponse;
import com.enterprise.oms.shipping.dto.UpdateShipmentRequest;
import com.enterprise.oms.shipping.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final OrderService orderService;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional
    public ShipmentResponse createShipment(UUID orderId) {
        log.info("Creating shipment for Order ID: {}", orderId);
        
        String trackingNumber = "TRK-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String carrier = "FedEx Express";

        Shipment shipment = Shipment.builder()
                .orderId(orderId)
                .trackingNumber(trackingNumber)
                .carrier(carrier)
                .status(ShipmentStatus.CREATED)
                .estimatedDelivery(Instant.now().plus(3, ChronoUnit.DAYS))
                .build();

        shipment = shipmentRepository.save(shipment);

        orderService.transitionOrderStatus(orderId, OrderStatus.SHIPPED, "Shipment dispatched with tracking " + trackingNumber);

        ShipmentCreatedEvent event = ShipmentCreatedEvent.builder()
                .shipmentId(shipment.getId())
                .orderId(orderId)
                .trackingNumber(trackingNumber)
                .carrier(carrier)
                .build();

        kafkaEventPublisher.publishEvent(KafkaConfig.TOPIC_SHIPPING_EVENTS, shipment.getId().toString(), event);

        return mapToResponse(shipment);
    }

    @Transactional
    public ShipmentResponse updateShipmentStatus(UUID shipmentId, UpdateShipmentRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        shipment.setStatus(request.getStatus());
        shipment = shipmentRepository.save(shipment);

        if (request.getStatus() == ShipmentStatus.OUT_FOR_DELIVERY) {
            orderService.transitionOrderStatus(shipment.getOrderId(), OrderStatus.OUT_FOR_DELIVERY, "Package out for delivery");
        } else if (request.getStatus() == ShipmentStatus.DELIVERED) {
            orderService.transitionOrderStatus(shipment.getOrderId(), OrderStatus.DELIVERED, "Package delivered successfully");

            ShipmentDeliveredEvent event = ShipmentDeliveredEvent.builder()
                    .shipmentId(shipment.getId())
                    .orderId(shipment.getOrderId())
                    .build();
            kafkaEventPublisher.publishEvent(KafkaConfig.TOPIC_SHIPPING_EVENTS, shipment.getId().toString(), event);
        }

        return mapToResponse(shipment);
    }

    public ShipmentResponse getShipmentByOrderId(UUID orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order ID: " + orderId));
        return mapToResponse(shipment);
    }

    public ShipmentResponse mapToResponse(Shipment shipment) {
        return ShipmentResponse.builder()
                .id(shipment.getId())
                .orderId(shipment.getOrderId())
                .trackingNumber(shipment.getTrackingNumber())
                .carrier(shipment.getCarrier())
                .status(shipment.getStatus())
                .estimatedDelivery(shipment.getEstimatedDelivery())
                .createdAt(shipment.getCreatedAt())
                .updatedAt(shipment.getUpdatedAt())
                .build();
    }
}

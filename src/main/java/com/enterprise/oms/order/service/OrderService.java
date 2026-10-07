package com.enterprise.oms.order.service;

import com.enterprise.oms.cart.service.CartService;
import com.enterprise.oms.common.config.KafkaConfig;
import com.enterprise.oms.common.event.OrderCancelledEvent;
import com.enterprise.oms.common.event.OrderCreatedEvent;
import com.enterprise.oms.common.exception.ResourceNotFoundException;
import com.enterprise.oms.common.exception.UnauthorizedAccessException;
import com.enterprise.oms.messaging.producer.KafkaEventPublisher;
import com.enterprise.oms.order.domain.Order;
import com.enterprise.oms.order.domain.OrderEventLog;
import com.enterprise.oms.order.domain.OrderItem;
import com.enterprise.oms.order.domain.OrderStatus;
import com.enterprise.oms.order.dto.*;
import com.enterprise.oms.order.repository.OrderEventLogRepository;
import com.enterprise.oms.order.repository.OrderRepository;
import com.enterprise.oms.order.statemachine.OrderStateMachine;
import com.enterprise.oms.product.domain.Product;
import com.enterprise.oms.product.repository.ProductRepository;
import com.enterprise.oms.user.domain.Role;
import com.enterprise.oms.user.domain.User;
import com.enterprise.oms.user.service.UserService;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventLogRepository orderEventLogRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final CartService cartService;
    private final OrderStateMachine orderStateMachine;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Autowired(required = false)
    private Counter ordersCreatedCounter;

    @Autowired(required = false)
    private Counter ordersCompletedCounter;

    @Transactional
    public OrderResponse createOrder(String userEmail, CreateOrderRequest request) {
        User user = userService.getUserEntityByEmail(userEmail);
        String orderNumber = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderCreatedEvent.OrderItemEventPayload> eventPayloadItems = new ArrayList<>();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .status(OrderStatus.CREATED)
                .shippingAddress(request.getShippingAddress())
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemReq.getProductId()));

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(itemTotal)
                    .build();

            orderItems.add(orderItem);

            eventPayloadItems.add(OrderCreatedEvent.OrderItemEventPayload.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(product.getPrice())
                    .build());
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // Record initial event log
        recordEventLog(savedOrder.getId(), null, OrderStatus.CREATED, "Order initialized");

        // Clear user cart
        cartService.clearCart(userEmail);

        // Increment Micrometer metric
        if (ordersCreatedCounter != null) {
            ordersCreatedCounter.increment();
        }

        // Publish Kafka OrderCreated event
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .customerId(user.getId())
                .totalAmount(savedOrder.getTotalAmount())
                .items(eventPayloadItems)
                .build();

        kafkaEventPublisher.publishEvent(KafkaConfig.TOPIC_ORDER_EVENTS, savedOrder.getId().toString(), event);

        return mapToResponse(savedOrder);
    }

    @Transactional
    public Order transitionOrderStatus(UUID orderId, OrderStatus targetStatus, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        orderStateMachine.validateTransition(currentStatus, targetStatus);

        order.setStatus(targetStatus);
        Order updatedOrder = orderRepository.save(order);

        recordEventLog(orderId, currentStatus, targetStatus, reason);

        if (targetStatus == OrderStatus.DELIVERED && ordersCompletedCounter != null) {
            ordersCompletedCounter.increment();
        }

        return updatedOrder;
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String userEmail, String reason) {
        User user = userService.getUserEntityByEmail(userEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (user.getRole() == Role.CUSTOMER && !order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("Cannot cancel order belonging to another customer");
        }

        transitionOrderStatus(orderId, OrderStatus.CANCELLED, reason != null ? reason : "Cancelled by user");

        OrderCancelledEvent cancelledEvent = OrderCancelledEvent.builder()
                .orderId(order.getId())
                .customerId(order.getUser().getId())
                .reason(reason)
                .build();

        kafkaEventPublisher.publishEvent(KafkaConfig.TOPIC_ORDER_EVENTS, order.getId().toString(), cancelledEvent);

        return mapToResponse(orderRepository.findById(orderId).orElseThrow());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId, String userEmail) {
        User user = userService.getUserEntityByEmail(userEmail);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (user.getRole() == Role.CUSTOMER && !order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("Access denied to customer order");
        }

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getCustomerOrders(String userEmail, Pageable pageable) {
        User user = userService.getUserEntityByEmail(userEmail);
        return orderRepository.findByUserId(user.getId(), pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(this::mapToResponse);
    }

    private void recordEventLog(UUID orderId, OrderStatus from, OrderStatus to, String reason) {
        OrderEventLog eventLog = OrderEventLog.builder()
                .orderId(orderId)
                .fromStatus(from)
                .toStatus(to)
                .reason(reason)
                .build();
        orderEventLogRepository.save(eventLog);
    }

    public OrderResponse mapToResponse(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> OrderItemDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productSku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
                .toList();

        List<OrderTimelineEventDto> timeline = orderEventLogRepository.findByOrderIdOrderByCreatedAtAsc(order.getId())
                .stream()
                .map(log -> OrderTimelineEventDto.builder()
                        .id(log.getId())
                        .fromStatus(log.getFromStatus())
                        .toStatus(log.getToStatus())
                        .reason(log.getReason())
                        .timestamp(log.getCreatedAt())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getUser().getId())
                .customerEmail(order.getUser().getEmail())
                .customerName(order.getUser().getFullName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .items(itemDtos)
                .timeline(timeline)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

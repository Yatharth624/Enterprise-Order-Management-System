package com.enterprise.oms.service;

import com.enterprise.oms.cart.service.CartService;
import com.enterprise.oms.common.exception.ResourceNotFoundException;
import com.enterprise.oms.common.exception.UnauthorizedAccessException;
import com.enterprise.oms.messaging.producer.KafkaEventPublisher;
import com.enterprise.oms.order.domain.Order;
import com.enterprise.oms.order.domain.OrderStatus;
import com.enterprise.oms.order.dto.CreateOrderRequest;
import com.enterprise.oms.order.dto.OrderResponse;
import com.enterprise.oms.order.repository.OrderEventLogRepository;
import com.enterprise.oms.order.repository.OrderRepository;
import com.enterprise.oms.order.service.OrderService;
import com.enterprise.oms.order.statemachine.OrderStateMachine;
import com.enterprise.oms.product.domain.Product;
import com.enterprise.oms.product.repository.ProductRepository;
import com.enterprise.oms.user.domain.Role;
import com.enterprise.oms.user.domain.User;
import com.enterprise.oms.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderEventLogRepository orderEventLogRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserService userService;
    @Mock
    private CartService cartService;
    @Spy
    private OrderStateMachine orderStateMachine = new OrderStateMachine();
    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User customer1;
    private User customer2;
    private Product product;

    @BeforeEach
    void setUp() {
        customer1 = User.builder().id(UUID.randomUUID()).email("c1@test.com").fullName("Cust 1").role(Role.CUSTOMER).build();
        customer2 = User.builder().id(UUID.randomUUID()).email("c2@test.com").fullName("Cust 2").role(Role.CUSTOMER).build();
        product = Product.builder().id(UUID.randomUUID()).name("Test Phone").price(new BigDecimal("500.00")).sku("SKU-1").build();
    }

    @Test
    @DisplayName("Should successfully create order for authenticated customer")
    void testCreateOrderSuccess() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .shippingAddress("123 Main St")
                .items(List.of(CreateOrderRequest.OrderItemRequest.builder()
                        .productId(product.getId())
                        .quantity(2)
                        .build()))
                .build();

        when(userService.getUserEntityByEmail(customer1.getEmail())).thenReturn(customer1);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        OrderResponse response = orderService.createOrder(customer1.getEmail(), request);

        assertNotNull(response);
        assertEquals(OrderStatus.CREATED, response.getStatus());
        assertEquals(new BigDecimal("1000.00"), response.getTotalAmount());
        verify(kafkaEventPublisher, times(1)).publishEvent(anyString(), anyString(), any());
        verify(cartService, times(1)).clearCart(customer1.getEmail());
    }

    @Test
    @DisplayName("Should reject access when customer attempts to view another customer's order")
    void testUnauthorizedOrderAccess() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .user(customer1)
                .status(OrderStatus.CREATED)
                .totalAmount(new BigDecimal("500.00"))
                .shippingAddress("Address 1")
                .build();

        when(userService.getUserEntityByEmail(customer2.getEmail())).thenReturn(customer2);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(UnauthorizedAccessException.class, () ->
                orderService.getOrderById(orderId, customer2.getEmail()));
    }
}

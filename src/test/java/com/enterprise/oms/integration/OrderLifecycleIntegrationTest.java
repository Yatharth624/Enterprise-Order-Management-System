package com.enterprise.oms.integration;

import com.enterprise.oms.EnterpriseOmsApplication;
import com.enterprise.oms.order.domain.OrderStatus;
import com.enterprise.oms.order.dto.CreateOrderRequest;
import com.enterprise.oms.order.dto.OrderResponse;
import com.enterprise.oms.order.service.OrderService;
import com.enterprise.oms.payment.dto.PaymentResponse;
import com.enterprise.oms.payment.dto.ProcessPaymentRequest;
import com.enterprise.oms.payment.service.PaymentService;
import com.enterprise.oms.product.dto.ProductResponse;
import com.enterprise.oms.product.service.ProductService;
import com.enterprise.oms.shipping.dto.ShipmentResponse;
import com.enterprise.oms.shipping.dto.UpdateShipmentRequest;
import com.enterprise.oms.shipping.domain.ShipmentStatus;
import com.enterprise.oms.shipping.service.ShippingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = EnterpriseOmsApplication.class)
@ActiveProfiles("test")
@Transactional
class OrderLifecycleIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ShippingService shippingService;

    @Test
    @DisplayName("Complete Business Flow: Customer -> Product -> Order -> Reserve -> Payment -> Confirm -> Ship -> Deliver")
    void testCompleteOrderLifecycleFlow() {
        // 1. Fetch available product from seed data
        ProductResponse product = productService.searchProducts("MacBook", null, PageRequest.of(0, 10)).getContent().get(0);
        assertNotNull(product);

        // 2. Create Order
        CreateOrderRequest orderReq = CreateOrderRequest.builder()
                .shippingAddress("777 Enterprise Way, San Jose CA")
                .items(List.of(CreateOrderRequest.OrderItemRequest.builder()
                        .productId(product.getId())
                        .quantity(1)
                        .build()))
                .build();

        OrderResponse order = orderService.createOrder("customer@enterprise.com", orderReq);
        assertNotNull(order.getId());
        assertEquals(OrderStatus.CREATED, order.getStatus());

        // 3. Reserve Inventory & Transition to INVENTORY_RESERVED
        orderService.transitionOrderStatus(order.getId(), OrderStatus.INVENTORY_RESERVED, "Stock reserved");
        OrderResponse reservedOrder = orderService.getOrderById(order.getId(), "customer@enterprise.com");
        assertEquals(OrderStatus.INVENTORY_RESERVED, reservedOrder.getStatus());

        // 4. Move to PAYMENT_PENDING & Process Successful Payment
        orderService.transitionOrderStatus(order.getId(), OrderStatus.PAYMENT_PENDING, "Payment initiated");
        
        ProcessPaymentRequest payReq = ProcessPaymentRequest.builder()
                .orderId(order.getId())
                .amount(order.getTotalAmount())
                .paymentMethod("CREDIT_CARD")
                .simulateMode("FORCE_SUCCESS")
                .build();

        PaymentResponse payment = paymentService.processPayment(payReq, "TEST-IDEM-KEY-999");
        assertNotNull(payment);
        assertEquals(com.enterprise.oms.payment.domain.PaymentStatus.SUCCESS, payment.getStatus());

        // Transition order through payment confirmation
        orderService.transitionOrderStatus(order.getId(), OrderStatus.PAID, "Payment succeeded");
        orderService.transitionOrderStatus(order.getId(), OrderStatus.CONFIRMED, "Order confirmed");

        // 5. Create Shipment & Deliver
        ShipmentResponse shipment = shippingService.createShipment(order.getId());
        assertNotNull(shipment);
        assertNotNull(shipment.getTrackingNumber());

        shippingService.updateShipmentStatus(shipment.getId(), UpdateShipmentRequest.builder().status(ShipmentStatus.OUT_FOR_DELIVERY).build());
        shippingService.updateShipmentStatus(shipment.getId(), UpdateShipmentRequest.builder().status(ShipmentStatus.DELIVERED).build());

        // 6. Verify Final Order State & State Machine Audit Timeline Log
        OrderResponse finalOrder = orderService.getOrderById(order.getId(), "customer@enterprise.com");
        assertEquals(OrderStatus.DELIVERED, finalOrder.getStatus());
        assertTrue(finalOrder.getTimeline().size() >= 5);
    }
}

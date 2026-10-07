package com.enterprise.oms.statemachine;

import com.enterprise.oms.common.exception.InvalidStateTransitionException;
import com.enterprise.oms.order.domain.OrderStatus;
import com.enterprise.oms.order.statemachine.OrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    @Test
    @DisplayName("Should allow valid state transition CREATED -> INVENTORY_RESERVED")
    void testValidTransitionCreatedToInventoryReserved() {
        assertTrue(stateMachine.canTransition(OrderStatus.CREATED, OrderStatus.INVENTORY_RESERVED));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.CREATED, OrderStatus.INVENTORY_RESERVED));
    }

    @Test
    @DisplayName("Should allow valid state transition PAYMENT_PENDING -> PAID")
    void testValidTransitionPaymentPendingToPaid() {
        assertTrue(stateMachine.canTransition(OrderStatus.PAYMENT_PENDING, OrderStatus.PAID));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.PAYMENT_PENDING, OrderStatus.PAID));
    }

    @Test
    @DisplayName("Should reject invalid state transition DELIVERED -> CREATED")
    void testInvalidTransitionDeliveredToCreated() {
        assertFalse(stateMachine.canTransition(OrderStatus.DELIVERED, OrderStatus.CREATED));
        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.validateTransition(OrderStatus.DELIVERED, OrderStatus.CREATED));
    }

    @Test
    @DisplayName("Should reject invalid state transition CREATED -> DELIVERED")
    void testInvalidTransitionCreatedToDelivered() {
        assertFalse(stateMachine.canTransition(OrderStatus.CREATED, OrderStatus.DELIVERED));
        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.validateTransition(OrderStatus.CREATED, OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Should reject any transition from terminal CANCELLED state")
    void testCancelledTerminalState() {
        assertFalse(stateMachine.canTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED));
        assertThrows(InvalidStateTransitionException.class, () ->
                stateMachine.validateTransition(OrderStatus.CANCELLED, OrderStatus.CONFIRMED));
    }
}

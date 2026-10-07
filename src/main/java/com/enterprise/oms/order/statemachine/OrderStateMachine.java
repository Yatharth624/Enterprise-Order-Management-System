package com.enterprise.oms.order.statemachine;

import com.enterprise.oms.common.exception.InvalidStateTransitionException;
import com.enterprise.oms.order.domain.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class OrderStateMachine {

    private final Map<OrderStatus, Set<OrderStatus>> validTransitions = new EnumMap<>(OrderStatus.class);

    public OrderStateMachine() {
        // CREATED can transition to INVENTORY_RESERVED, INVENTORY_FAILED, CANCELLED
        validTransitions.put(OrderStatus.CREATED, EnumSet.of(
                OrderStatus.INVENTORY_RESERVED,
                OrderStatus.INVENTORY_FAILED,
                OrderStatus.CANCELLED
        ));

        // INVENTORY_RESERVED can transition to PAYMENT_PENDING, PAYMENT_FAILED, CANCELLED
        validTransitions.put(OrderStatus.INVENTORY_RESERVED, EnumSet.of(
                OrderStatus.PAYMENT_PENDING,
                OrderStatus.PAYMENT_FAILED,
                OrderStatus.CANCELLED
        ));

        // PAYMENT_PENDING can transition to PAID, PAYMENT_FAILED, CANCELLED
        validTransitions.put(OrderStatus.PAYMENT_PENDING, EnumSet.of(
                OrderStatus.PAID,
                OrderStatus.PAYMENT_FAILED,
                OrderStatus.CANCELLED
        ));

        // PAID can transition to CONFIRMED, CANCELLED
        validTransitions.put(OrderStatus.PAID, EnumSet.of(
                OrderStatus.CONFIRMED,
                OrderStatus.CANCELLED
        ));

        // CONFIRMED can transition to SHIPPED, CANCELLED
        validTransitions.put(OrderStatus.CONFIRMED, EnumSet.of(
                OrderStatus.SHIPPED,
                OrderStatus.CANCELLED
        ));

        // SHIPPED can transition to OUT_FOR_DELIVERY
        validTransitions.put(OrderStatus.SHIPPED, EnumSet.of(
                OrderStatus.OUT_FOR_DELIVERY
        ));

        // OUT_FOR_DELIVERY can transition to DELIVERED
        validTransitions.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(
                OrderStatus.DELIVERED
        ));

        // Terminal states cannot transition further
        validTransitions.put(OrderStatus.DELIVERED, Collections.emptySet());
        validTransitions.put(OrderStatus.PAYMENT_FAILED, Collections.emptySet());
        validTransitions.put(OrderStatus.CANCELLED, Collections.emptySet());
        validTransitions.put(OrderStatus.INVENTORY_FAILED, Collections.emptySet());
    }

    public boolean canTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        Set<OrderStatus> allowed = validTransitions.get(currentStatus);
        return allowed != null && allowed.contains(targetStatus);
    }

    public void validateTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (!canTransition(currentStatus, targetStatus)) {
            log.error("Invalid state transition attempted from {} to {}", currentStatus, targetStatus);
            throw new InvalidStateTransitionException(
                    "Invalid order state transition from " + currentStatus + " to " + targetStatus
            );
        }
    }
}

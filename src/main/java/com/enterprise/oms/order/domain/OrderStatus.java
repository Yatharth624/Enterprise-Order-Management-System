package com.enterprise.oms.order.domain;

public enum OrderStatus {
    CREATED,
    INVENTORY_RESERVED,
    PAYMENT_PENDING,
    PAID,
    CONFIRMED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    
    // Failure / Terminal states
    PAYMENT_FAILED,
    CANCELLED,
    INVENTORY_FAILED
}

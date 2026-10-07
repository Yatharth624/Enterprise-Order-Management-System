package com.enterprise.oms.common.exception;

public class IdempotentConflictException extends RuntimeException {
    public IdempotentConflictException(String message) {
        super(message);
    }
}

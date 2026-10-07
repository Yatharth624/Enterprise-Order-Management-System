package com.enterprise.oms.common.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    String getEventId();
    String getEventType();
    Instant getTimestamp();
}

package com.enterprise.oms.messaging.producer;

import com.enterprise.oms.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishEvent(String topic, String key, DomainEvent event) {
        try {
            log.info("Publishing event [{}] to topic [{}] with key [{}]", event.getEventType(), topic, key);
            kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send Kafka event to topic [{}]: {}", topic, ex.getMessage());
                } else {
                    log.debug("Event [{}] sent successfully to partition [{}]", event.getEventType(), result.getRecordMetadata().partition());
                }
            });
        } catch (Exception e) {
            log.warn("Kafka event publishing fallback triggered for topic [{}]: {}", topic, e.getMessage());
        }
    }
}

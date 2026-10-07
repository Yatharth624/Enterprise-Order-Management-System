package com.enterprise.oms.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_ORDER_EVENTS = "order-events";
    public static final String TOPIC_INVENTORY_EVENTS = "inventory-events";
    public static final String TOPIC_PAYMENT_EVENTS = "payment-events";
    public static final String TOPIC_SHIPPING_EVENTS = "shipping-events";
    public static final String TOPIC_NOTIFICATION_EVENTS = "notification-events";
    public static final String TOPIC_DLQ = "oms-dlq";

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(TOPIC_ORDER_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(TOPIC_INVENTORY_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic shippingEventsTopic() {
        return TopicBuilder.name(TOPIC_SHIPPING_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationEventsTopic() {
        return TopicBuilder.name(TOPIC_NOTIFICATION_EVENTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic dlqTopic() {
        return TopicBuilder.name(TOPIC_DLQ).partitions(1).replicas(1).build();
    }
}

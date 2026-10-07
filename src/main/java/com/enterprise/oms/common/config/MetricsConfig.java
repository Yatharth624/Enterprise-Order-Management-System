package com.enterprise.oms.common.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter ordersCreatedCounter(MeterRegistry registry) {
        return Counter.builder("oms_orders_created_total")
                .description("Total number of orders created")
                .register(registry);
    }

    @Bean
    public Counter ordersCompletedCounter(MeterRegistry registry) {
        return Counter.builder("oms_orders_completed_total")
                .description("Total number of orders completed successfully")
                .register(registry);
    }

    @Bean
    public Counter paymentFailuresCounter(MeterRegistry registry) {
        return Counter.builder("oms_payment_failures_total")
                .description("Total number of payment failures")
                .register(registry);
    }

    @Bean
    public Counter inventoryFailuresCounter(MeterRegistry registry) {
        return Counter.builder("oms_inventory_failures_total")
                .description("Total number of inventory reservation failures")
                .register(registry);
    }

    @Bean
    public Timer orderProcessingTimer(MeterRegistry registry) {
        return Timer.builder("oms_order_processing_duration_seconds")
                .description("Time taken to process an order lifecycle")
                .register(registry);
    }
}

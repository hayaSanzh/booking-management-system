package com.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables async processing for event publishing.
 * This allows booking operations to complete without waiting for RabbitMQ.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

package com.booking.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for publishing booking events.
 * The notification-service consumes these events.
 */
@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange:booking.events}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue:booking.notifications}")
    private String queueName;

    @Value("${app.rabbitmq.routing-key:booking.#}")
    private String routingKey;

    /**
     * Topic exchange for booking events
     */
    @Bean
    public TopicExchange bookingExchange() {
        return ExchangeBuilder
                .topicExchange(exchangeName)
                .durable(true)
                .build();
    }

    /**
     * Main queue for notifications
     */
    @Bean
    public Queue bookingNotificationQueue() {
        return QueueBuilder
                .durable(queueName)
                .build();
    }

    /**
     * Binding between exchange and queue
     */
    @Bean
    public Binding bookingBinding(Queue bookingNotificationQueue, TopicExchange bookingExchange) {
        return BindingBuilder
                .bind(bookingNotificationQueue)
                .to(bookingExchange)
                .with(routingKey);
    }

    /**
     * JSON message converter for RabbitMQ
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}

package com.booking.event;

import com.booking.entity.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Publishes booking events to RabbitMQ for notification-service consumption.
 * Uses async processing to avoid blocking the main transaction.
 */
@Component
public class BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange:booking.events}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key:booking.created}")
    private String routingKey;

    @Value("${app.rabbitmq.enabled:true}")
    private boolean enabled;

    public BookingEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publish booking created event
     */
    @Async
    public void publishBookingCreated(Booking booking) {
        if (!enabled) {
            log.debug("RabbitMQ publishing is disabled, skipping event for booking {}", booking.getId());
            return;
        }

        BookingEvent event = BookingEvent.created(
                booking.getId(),
                booking.getUser().getEmail(),
                booking.getUser().getFullName(),
                booking.getResource().getName(),
                booking.getStartAt(),
                booking.getEndAt()
        );

        publishEvent(event, "booking.created");
    }

    /**
     * Publish booking canceled event
     */
    @Async
    public void publishBookingCanceled(Booking booking) {
        if (!enabled) {
            log.debug("RabbitMQ publishing is disabled, skipping event for booking {}", booking.getId());
            return;
        }

        BookingEvent event = BookingEvent.canceled(
                booking.getId(),
                booking.getUser().getEmail(),
                booking.getUser().getFullName(),
                booking.getResource().getName(),
                booking.getStartAt(),
                booking.getEndAt()
        );

        publishEvent(event, "booking.canceled");
    }

    private void publishEvent(BookingEvent event, String routingKey) {
        try {
            log.info("Publishing {} event: bookingId={}, eventId={}", 
                    event.getEventType(), event.getBookingId(), event.getEventId());
            
            rabbitTemplate.convertAndSend(exchange, routingKey, event);
            
            log.debug("Successfully published event {} to exchange '{}' with routing key '{}'",
                    event.getEventId(), exchange, routingKey);
        } catch (AmqpException e) {
            // Log error but don't fail the booking operation
            log.error("Failed to publish {} event for booking {}: {}",
                    event.getEventType(), event.getBookingId(), e.getMessage());
        }
    }
}

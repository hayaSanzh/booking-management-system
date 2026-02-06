# Booking Management System

REST API for booking resources (meeting rooms, equipment, etc.) with JWT authentication, conflict detection, and event-driven notifications.

![Java](https://img.shields.io/badge/Java-17+-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-orange)
![License](https://img.shields.io/badge/License-MIT-yellow)

## Features

- **JWT Authentication** — Secure login with 24-hour tokens
- **Role-Based Access** — USER and ADMIN roles
- **Booking System** — Create, view, cancel bookings with conflict detection
- **409 Conflict** — Prevents double-booking (overlapping time slots)
- **Event Publishing** — Sends booking events to RabbitMQ for notification-service
- **Validation** — Duration 15min–8h, future-only bookings
- **Filtering & Pagination** — On all list endpoints
- **Swagger UI** — Auto-generated API docs

## Architecture

```
┌─────────────────────┐     ┌─────────────────────┐
│ booking-management  │────▶│     RabbitMQ        │
│       system        │     │  booking.events     │
└─────────────────────┘     └──────────┬──────────┘
         │                             │
         ▼                             ▼
┌─────────────────────┐     ┌─────────────────────┐
│   PostgreSQL        │     │ notification-service│
│   (booking_db)      │     │   (Email sender)    │
└─────────────────────┘     └─────────────────────┘
```

## Quick Start

### Full Stack (with Notification Service)

```bash
# From parent directory (projects/)
docker compose up -d

# Services:
# - Booking API:     http://localhost:8080
# - Notification:    http://localhost:8081
# - RabbitMQ:        http://localhost:15672 (guest/guest)
# - MailHog:         http://localhost:8025
# - Swagger UI:      http://localhost:8080/swagger-ui.html
```

### Standalone (without notifications)

```bash
# Start infrastructure
docker compose up -d postgres rabbitmq

# Run application
./mvnw spring-boot:run
```

## API Endpoints

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/v1/auth/register` | Register user | Public |
| POST | `/api/v1/auth/login` | Get JWT token | Public |
| GET | `/api/v1/auth/me` | Current user | Auth |
| GET | `/api/v1/resources` | List resources | Auth |
| POST | `/api/v1/resources` | Create resource | ADMIN |
| PUT | `/api/v1/resources/{id}` | Update resource | ADMIN |
| DELETE | `/api/v1/resources/{id}` | Delete resource | ADMIN |
| GET | `/api/v1/bookings` | List bookings* | Auth |
| POST | `/api/v1/bookings` | Create booking | Auth |
| POST | `/api/v1/bookings/{id}/cancel` | Cancel booking | Owner/ADMIN |

> *USER sees only own bookings, ADMIN sees all

## Examples

### Register & Login

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"pass123","fullName":"John Doe"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"pass123"}'
```

### Create Booking

```bash
curl -X POST http://localhost:8080/api/v1/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "resourceId": 1,
    "startAt": "2026-02-10T10:00:00",
    "endAt": "2026-02-10T11:00:00",
    "description": "Team meeting"
  }'
```

### Conflict Response (409)

```json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "Booking conflict: the requested time slot overlaps with an existing booking"
}
```

## Configuration

| Environment Variable | Description | Default |
|---------------------|-------------|---------|
| `SPRING_DATASOURCE_URL` | Database URL | `jdbc:postgresql://localhost:5432/booking_db` |
| `JWT_SECRET` | JWT signing key (64+ chars) | dev key |
| `RABBITMQ_HOST` | RabbitMQ host | `localhost` |
| `RABBITMQ_PORT` | RabbitMQ port | `5672` |
| `RABBITMQ_ENABLED` | Enable event publishing | `true` |
| `APP_BOOKING_MIN_DURATION_MINUTES` | Min booking duration | `15` |
| `APP_BOOKING_MAX_DURATION_HOURS` | Max booking duration | `8` |

## Event Publishing

When a booking is created or canceled, an event is published to RabbitMQ:

```
Exchange: booking.events (topic)
Routing Keys:
  - booking.created
  - booking.canceled
```

**Event Payload:**
```json
{
  "eventId": "uuid",
  "eventType": "BOOKING_CREATED",
  "bookingId": 123,
  "userEmail": "user@example.com",
  "userFullName": "John Doe",
  "resourceName": "Meeting Room A",
  "startAt": "2026-02-10T10:00:00",
  "endAt": "2026-02-10T11:00:00",
  "timestamp": "2026-02-10T09:00:00"
}
```

## Testing

```bash
# Run all tests
./mvnw test

# Unit tests: 19 (BookingService, validations, overlap detection)
# Integration tests: 9 (Testcontainers, requires Docker)
```

## Tech Stack

- **Java 17** + Spring Boot 3.2
- **Spring Security** + JWT (jjwt 0.12.5)
- **Spring AMQP** + RabbitMQ
- **PostgreSQL 16** + Flyway migrations
- **Docker** + Testcontainers
- **SpringDoc OpenAPI** (Swagger UI)

## Project Structure

```
src/main/java/com/booking/
├── config/        # Security, RabbitMQ, OpenAPI config
├── controller/    # REST controllers
├── dto/           # Request/Response DTOs
├── entity/        # JPA entities
├── event/         # BookingEvent, BookingEventPublisher
├── exception/     # Exception handlers
├── repository/    # Data access layer
├── security/      # JWT auth filters
└── service/       # Business logic
```

## Booking Rules

1. **Duration:** 15 minutes – 8 hours
2. **Time:** Future bookings only
3. **Conflicts:** Overlapping bookings return 409
4. **Cancel:** Only owner or ADMIN, only future bookings

## License

MIT

# Booking Management System

REST API for booking resources (meeting rooms, equipment, etc.) with JWT authentication and conflict detection.

![Java](https://img.shields.io/badge/Java-17+-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

## Features

- **JWT Authentication** — Secure login with 24-hour tokens
- **Role-Based Access** — USER and ADMIN roles
- **Booking System** — Create, view, cancel bookings with conflict detection
- **409 Conflict** — Prevents double-booking (overlapping time slots)
- **Validation** — Duration 15min–8h, future-only bookings
- **Filtering & Pagination** — On all list endpoints
- **Swagger UI** — Auto-generated API docs

## Quick Start

```bash
# Start everything (PostgreSQL + App)
docker compose up -d

# Access API at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
```

### Local Development

```bash
# Start database only
docker compose up -d postgres

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
| `APP_BOOKING_MIN_DURATION_MINUTES` | Min booking duration | `15` |
| `APP_BOOKING_MAX_DURATION_HOURS` | Max booking duration | `8` |

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
- **PostgreSQL 16** + Flyway migrations
- **Docker** + Testcontainers
- **SpringDoc OpenAPI** (Swagger UI)

## Project Structure

```
src/main/java/com/booking/
├── config/        # Security, OpenAPI config
├── controller/    # REST controllers
├── dto/           # Request/Response DTOs
├── entity/        # JPA entities
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

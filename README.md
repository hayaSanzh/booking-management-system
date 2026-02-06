# Booking Management System (BMS)

A production-ready REST API for booking resources (meeting rooms, equipment, etc.).

Built with **Spring Boot 3.2**, **PostgreSQL**, **JWT authentication**, and **Docker**.

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)

---

## Features

- **JWT Authentication** — Secure login with 24-hour tokens
- **Role-Based Access** — USER and ADMIN roles
- **Resource Management** — CRUD for bookable resources (ADMIN only)
- **Booking System** — Create, view, and cancel bookings
- **Conflict Detection** — Prevents double-booking with overlap check
- **Validation** — Duration limits (15min–8h), future-only bookings
- **Soft Delete** — Resources and canceled bookings are preserved
- **Filtering & Pagination** — On all list endpoints
- **OpenAPI/Swagger** — Auto-generated API documentation

---

## Architecture

\`\`\`
┌─────────────────────────────────────────────────────────────────┐
│                        REST API Layer                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │   Auth   │  │  Users   │  │Resources │  │    Bookings      │ │
│  │Controller│  │Controller│  │Controller│  │   Controller     │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └───────┬──────────┘ │
└───────┼─────────────┼───────────────┼──────────────┼────────────┘
        │             │               │              │
┌───────▼─────────────▼───────────────▼──────────────▼────────────┐
│                       Service Layer                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐ │
│  │   Auth   │  │   User   │  │ Resource │  │     Booking      │ │
│  │ Service  │  │ Service  │  │ Service  │  │    Service       │ │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └───────┬──────────┘ │
└───────┼─────────────┼───────────────┼──────────────┼────────────┘
        │             │               │              │
┌───────▼─────────────▼───────────────▼──────────────▼────────────┐
│                     Repository Layer (JPA)                       │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────▼─────────┐
                    │   PostgreSQL 16   │
                    └───────────────────┘
\`\`\`

---

## Quick Start

### One Command Run

\`\`\`bash
docker compose up -d
\`\`\`

This starts both PostgreSQL and the application. Access at http://localhost:8080

### Local Development

\`\`\`bash
# Start database only
docker compose up -d postgres

# Run application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
\`\`\`

### Run Tests

\`\`\`bash
./mvnw test
\`\`\`

---

## 📚 API Documentation

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

### Endpoints

| Endpoint | Method | Description | Access |
|----------|--------|-------------|--------|
| \`/api/v1/auth/register\` | POST | Register new user | Public |
| \`/api/v1/auth/login\` | POST | Login, get JWT | Public |
| \`/api/v1/auth/me\` | GET | Current user info | Auth |
| \`/api/v1/users/me\` | GET/PATCH | User profile | Auth |
| \`/api/v1/resources\` | GET | List resources | Auth |
| \`/api/v1/resources/{id}\` | GET | Get resource | Auth |
| \`/api/v1/resources\` | POST | Create resource | ADMIN |
| \`/api/v1/resources/{id}\` | PUT/DELETE | Manage resource | ADMIN |
| \`/api/v1/bookings\` | GET | List bookings* | Auth |
| \`/api/v1/bookings/{id}\` | GET | Get booking | Owner/ADMIN |
| \`/api/v1/bookings\` | POST | Create booking | Auth |
| \`/api/v1/bookings/{id}/cancel\` | POST | Cancel booking | Owner/ADMIN |

*USER sees only own bookings, ADMIN sees all

---

## 📝 API Examples

### Register
\`\`\`bash
curl -X POST http://localhost:8080/api/v1/auth/register \\
  -H "Content-Type: application/json" \\
  -d '{"email":"user@example.com","password":"pass123","fullName":"Nurmukhanbet Sanzhar"}'
\`\`\`

### Login
\`\`\`bash
curl -X POST http://localhost:8080/api/v1/auth/login \\
  -H "Content-Type: application/json" \\
  -d '{"email":"user@example.com","password":"pass123"}'
\`\`\`

### Create Booking
\`\`\`bash
curl -X POST http://localhost:8080/api/v1/bookings \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer YOUR_TOKEN" \\
  -d '{"resourceId":1,"startAt":"2026-02-10T10:00:00","endAt":"2026-02-10T11:00:00"}'
\`\`\`

### Conflict Response (409)
\`\`\`json
{
  "timestamp": "2026-02-06T12:00:00",
  "status": 409,
  "error": "CONFLICT",
  "message": "Booking conflict: the requested time slot overlaps",
  "path": "/api/v1/bookings"
}
\`\`\`

---

## ⚙️ Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| \`SPRING_DATASOURCE_URL\` | Database URL | \`jdbc:postgresql://localhost:5432/booking_db\` |
| \`JWT_SECRET\` | JWT signing key (64+ chars) | dev key |
| \`APP_BOOKING_MIN_DURATION_MINUTES\` | Min duration | \`15\` |
| \`APP_BOOKING_MAX_DURATION_HOURS\` | Max duration | \`8\` |

---

## 🧪 Testing

### Unit Tests
- BookingService: overlap detection, validations, cancel logic

### Integration Tests (Testcontainers)
- Full API with real PostgreSQL
- 409 Conflict scenarios
- Role-based access

---

## 🔒 Booking Rules

1. **Duration:** 15 min – 8 hours
2. **Time:** Future only
3. **Conflicts:** \`new.startAt < existing.endAt && new.endAt > existing.startAt\`
4. **Cancel:** Owner or ADMIN, future bookings only

---

## 🛠️ Tech Stack

- Java 17, Spring Boot 3.2.2
- Spring Security + JWT
- PostgreSQL 16 + Flyway
- Docker, Testcontainers
- SpringDoc OpenAPI

---

## 📁 Structure

\`\`\`
src/main/java/com/booking/
├── config/        # Security, OpenAPI
├── controller/    # REST endpoints
├── dto/           # Request/Response
├── entity/        # JPA entities
├── exception/     # Error handling
├── repository/    # Data access
├── security/      # JWT, filters
└── service/       # Business logic
\`\`\`

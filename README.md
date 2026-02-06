# Booking Management System (BMS)

A monolithic REST API for resource booking.

## Requirements

- Java 17+
- Docker & Docker Compose
- Maven 3.8+

## Quick Start

### 1. Run with Docker Compose
```bash
docker-compose up -d
```

### 2. Local Development

```bash
docker-compose up -d postgres

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Available URLs

- **API:** http://localhost:8080/api/v1
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

## Profiles

- `dev` — development (verbose logging, local DB)
- `test` — testing (H2 in-memory)

## Project Structure

```
src/main/java/com/booking/
├── config/          # Configuration (Security, OpenAPI)
├── controller/      # REST controllers
├── dto/             # Data Transfer Objects
├── entity/          # JPA entities
├── exception/       # Exception handling
├── repository/      # JPA repositories
├── service/         # Business logic
└── security/        # JWT, filters
```

## Database

- PostgreSQL 16
- Flyway migrations in `src/main/resources/db/migration/`

---

## API Endpoints

### Auth API (`/api/v1/auth`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/register` | Register a new user | Public |
| POST | `/login` | Authenticate, get JWT | Public |
| GET | `/me` | Current user info | Authenticated |

### User API (`/api/v1/users`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/me` | Current user profile | Authenticated |
| PATCH | `/me` | Update profile (fullName) | Authenticated |

### Resource API (`/api/v1/resources`)

| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/` | List resources (filters + pagination) | Authenticated |
| GET | `/{id}` | Get resource by ID | Authenticated |
| POST | `/` | Create resource | ADMIN |
| PUT | `/{id}` | Update resource | ADMIN |
| DELETE | `/{id}` | Delete resource (soft delete) | ADMIN |

**Filters for GET /resources:**
- `name` — search by name (LIKE)
- `location` — search by location (LIKE)
- `capacityMin` — minimum capacity
- `page`, `size`, `sort` — pagination

---

## Authentication

JWT token (24 hours). Pass in header:
```
Authorization: Bearer <token>
```

### Roles
- **USER** — book resources
- **ADMIN** — manage resources + all USER permissions

---

## Error Format

All errors are returned in a unified format:

```json
{
  "timestamp": "2026-02-06T12:00:00",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Validation failed",
  "path": "/api/v1/resources",
  "errors": [
    {"field": "name", "message": "Name is required"}
  ]
}
```


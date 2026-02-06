# Booking Management System (BMS)

Монолитное REST API для бронирования ресурсов (переговорных комнат).

## Требования

- Java 17+
- Docker & Docker Compose
- Maven 3.8+

## Быстрый старт

### 1. Запуск через Docker Compose (рекомендуется)

```bash
# Поднять только PostgreSQL
docker-compose up -d postgres

# Или поднять всё (БД + приложение)
docker-compose up -d
```

### 2. Локальный запуск

```bash
# Поднять PostgreSQL
docker-compose up -d postgres

# Запустить приложение
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Доступные URL

- **API:** http://localhost:8080/api/v1
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

## Профили

- `dev` — для разработки (подробные логи, локальная БД)
- `test` — для тестов (H2 in-memory)

## Структура проекта

```
src/main/java/com/booking/
├── config/          # Конфигурации (Security, OpenAPI)
├── controller/      # REST контроллеры
├── dto/             # Data Transfer Objects
├── entity/          # JPA сущности
├── exception/       # Обработка исключений
├── repository/      # JPA репозитории
├── service/         # Бизнес-логика
└── security/        # JWT, фильтры
```

## База данных

- PostgreSQL 16
- Flyway миграции в `src/main/resources/db/migration/`

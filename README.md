# Linkforge - URL Shortener Backend

A scalable, high-performance URL shortener backend built with Kotlin and Spring Boot 3.

## Tech Stack
* **Language:** Kotlin
* **Framework:** Spring Boot 3
* **Database:** PostgreSQL
* **Migrations:** Flyway
* **API Documentation:** SpringDoc OpenAPI (Swagger)
* **Containerization:** Docker Compose

## Prerequisites
* Java 21+
* Docker and Docker Compose
* Gradle

## Getting Started

1. **Start the database:**
   ```bash
   docker-compose up -d
   ```

2. **Run the application:**
   ```bash
   gradle bootRun
   ```

3. **Access API Documentation:**
   Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) in your browser.

4. **Health Check:**
   [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

## Features
* **Shorten URL:** Convert long URLs into concise, shareable short codes.
* **Fast Redirection:** Quick lookups utilizing indexed short codes to return 302 Found redirects.
* **Duplicate Prevention:** Optimized checks using indexed original URLs to prevent redundant records.
* **Validation & Exception Handling:** Global exception handling for validation (`@URL`) and application exceptions (400, 404).

## Configuration

Linkforge supports multiple **Short Code Generation Strategies**, configurable dynamically via `application.yml` or environment variables without requiring code changes.

### Generation Strategies:
* `sequential` (default): Uses auto-incrementing database IDs encoded in Base62. Safe and ensures optimal density.
* `random`: Generates a randomized 7-character Base62 string. Features out-of-the-box collision handling and retries.

Configure the strategy in `application.yml`:
```yaml
app:
  shortener:
    strategy: random # Options: sequential, random
```
Or via environment variable:
```bash
SHORTENER_STRATEGY=random gradle bootRun
```

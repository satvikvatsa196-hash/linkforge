# Linkforge - URL Shortener Backend

A scalable, high-performance URL shortener backend built with Kotlin and Spring Boot 3.

## Tech Stack
* **Language:** Kotlin
* **Framework:** Spring Boot 3
* **Database:** PostgreSQL
* **Caching:** Redis
* **Migrations:** Flyway
* **API Documentation:** SpringDoc OpenAPI (Swagger)
* **Containerization:** Docker Compose

## Prerequisites
* Java 21+
* Docker and Docker Compose
* Gradle

## Getting Started

1. **Configure Environment:**
   ```bash
   cp .env.example .env
   ```
   *(Adjust `.env` parameters if needed).*

2. **Start the infrastructure (PostgreSQL & Redis):**
   ```bash
   docker-compose up -d
   ```

3. **Run the application:**
   ```bash
   ./gradlew bootRun
   ```

4. **Access API Documentation:**
   Open [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) in your browser.

5. **Health Check:**
   [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

## Features
* **Shorten URL:** Convert long URLs into concise, shareable short codes.
* **Fast Redirection & Caching:** Extremely fast lookups utilizing Redis as a primary cache (Cache-Aside pattern) with asynchronous writes and seamless fallback to PostgreSQL.
* **Duplicate Prevention:** Optimized checks using indexed original URLs to prevent redundant records.
* **Validation & Exception Handling:** Global exception handling for validation (`@URL`) and application exceptions (400, 404).

## URL Expiration
Linkforge supports setting an expiration time when generating a short URL:
* **Creation:** Provide an `expiresAt` ISO-8601 timestamp in the request body (e.g. `"expiresAt": "2026-09-01T12:00:00Z"`). If omitted, the URL never expires.
* **Redirect Behavior:** 
  - Active URLs redirect with `302 Found`.
  - Expired URLs return `410 Gone` and will not redirect.
  - Redis cache strictly respects expiration via native TTL and will never bypass expiration semantics.
* **Cleanup:** A scheduled background task automatically identifies expired URLs, marks them as inactive, and completely invalidates their Redis cache entries, while keeping the core database record for historical purposes.

## Configuration

Linkforge is configured primarily via environment variables. Start by copying `.env.example` to `.env`. 

### Cache Configuration
The Redis cache automatically populates and evicts data based on a configured Time-To-Live (TTL):
```yaml
app:
  cache:
    ttl: 24h
```
Override via `.env` with `CACHE_TTL=24h`.

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

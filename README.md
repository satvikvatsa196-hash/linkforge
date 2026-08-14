# Linkforge - URL Shortener Backend

A scalable, high-performance URL shortener backend built with Kotlin and Spring Boot 3.

## Tech Stack
* **Language:** Kotlin
* **Framework:** Spring Boot 3
* **Database:** PostgreSQL
* **Caching:** Redis
* **Message Broker:** RabbitMQ
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
* **Custom Aliases:** Specify your own custom short code (e.g. `my-brand`) instead of using generated ones.
* **Fast Redirection & Caching:** Extremely fast lookups utilizing Redis as a primary cache (Cache-Aside pattern) with asynchronous writes and seamless fallback to PostgreSQL.
* **Duplicate Prevention:** Optimized checks using indexed original URLs to prevent redundant records.
* **Analytics & Click Tracking:** Captures detailed usage metrics for shortened URLs including total clicks, first/last click times, and daily click distributions.
* **Dashboard APIs:** Comprehensive dashboard-oriented endpoints for analyzing link performance over arbitrary date ranges (top URLs, referrers, unique visitors, browser/device breakdowns).
* **QR Code Generation:** Generate downloadable QR codes (`image/png`) for any active shortened URL lazily upon request, cached in Redis for fast retrieval.
* **Rate Limiting:** Protects heavy API operations using an anonymous sliding-window rate limit backed by Redis Lua scripts, preserving core redirect availability.
* **Validation & Exception Handling:** Global exception handling for validation (`@URL`) and application exceptions (400, 404, 409, 410).
* **Custom Domains:** Support for fully-isolated short links running on custom domains.

## Custom Domains
Linkforge offers natively isolated domain resolution. 
* **Creation:** Supply a `domain` string in the JSON payload alongside the original URL. The system validates syntax (e.g. `go.brand.com`) and automatically registers unique domains.
* **Resolution Isolation:** The redirection strictly evaluates the `Host` header (`request.serverName`). This allows the **exact same alias** (e.g., `docs`) to exist simultaneously across different domains (e.g., `linkforge.example.com/docs` and `go.brand.com/docs`).
* **Deactivation:** Custom domains can be toggled inactive in the database (`active = false`), instantly returning `404 Not Found` for all links tied to them.
* **Caching:** The Redis cache architecture builds highly specific cache keys (e.g. `url:go.brand.com:docs` vs `url:docs`) ensuring zero collisions in-memory.

## URL Expiration
Linkforge supports setting an expiration time when generating a short URL:
* **Creation:** Provide an `expiresAt` ISO-8601 timestamp in the request body (e.g. `"expiresAt": "2026-09-01T12:00:00Z"`). If omitted, the URL never expires.
* **Redirect Behavior:** 
  - Active URLs redirect with `302 Found`.
  - Expired URLs return `410 Gone` and will not redirect.
  - Redis cache strictly respects expiration via native TTL and will never bypass expiration semantics.
* **Cleanup:** A scheduled background task automatically identifies expired URLs, marks them as inactive, and completely invalidates their Redis cache entries, while keeping the core database record for historical purposes.

## Rate Limiting
To prevent abuse of heavy operations, Linkforge implements API rate limiting applied selectively to URL creation (`POST /api/v1/urls`) and analytics (`/api/v1/analytics/**`) endpoints. Crucially, redirect operations are excluded to ensure high availability for link redirection.
* **Algorithm:** Implements a sliding-window algorithm utilizing Redis sorted sets (`ZSET`) via a Lua script for strict atomicity and accurate rate enforcement.
* **Identity:** Identifies clients by their IP address. To preserve user privacy, raw IP addresses are never permanently stored; they are synchronously hashed using SHA-256 before interacting with Redis.
* **Failure Behavior (Fail-Open):** In the event of a Redis failure, the rate limiting service fails *open* (allowing requests through) while logging the failure. This tradeoff prioritizes system availability over strict enforcement.
* **Headers:** Requests exceeding limits receive a `429 Too Many Requests` status, complete with a dynamically calculated `Retry-After` header indicating exactly when the next request will be permitted.

## Analytics & Click Tracking
Linkforge records successful redirects to provide engagement analytics, accessible via `/api/v1/analytics/**`.
* **Asynchronous Processing:** To ensure redirect latency is unaffected by database writes, click tracking is completely offloaded to a RabbitMQ message broker. The redirect returns immediately, making analytics eventually consistent. Durable queues, DLQ (Dead-Letter Queues), and retry mechanisms guarantee reliable delivery even in the event of database outages.
* **Captured Data:** Timestamp, anonymized IP address, User-Agent, and Referrer. 
* **Privacy & IP Hashing:** To protect user privacy and comply with data protection regulations, raw IP addresses are **never** stored permanently. Instead, they are synchronously hashed using SHA-256 with a configurable salt (`app.security.ip-salt`) prior to being published to RabbitMQ. This ensures unique visitor tracking is possible for analytics without exposing PII (Personally Identifiable Information).
* **Dashboard APIs:** Comprehensive endpoints support advanced aggregation using efficient, native SQL (`DATE_TRUNC`, `CASE`) to avoid loading events into application memory. Endpoints include:
  - `/overview`: Total clicks, unique visitors, active URLs, total URLs.
  - `/performance`: Paginated list of top URLs by traffic.
  - `/trends`: Click trends bucketed dynamically by `hourly`, `daily`, or `weekly` intervals.
  - `/urls/{shortCode}`: Detailed per-URL analytics parsing referrers, browsers, and devices.

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

### QR Code Configuration
Customize the dimensions of the generated QR codes:
```yaml
app:
  qr:
    width: 250
    height: 250
```
Override via `.env` with `QR_WIDTH=250` and `QR_HEIGHT=250`.

### Security Configuration
Configure the salt used for hashing IP addresses in the click tracking module to ensure privacy:
```yaml
app:
  security:
    ip-salt: default-salt-value-for-dev
```
Override via `.env` with `IP_SALT=your-secure-salt`.

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

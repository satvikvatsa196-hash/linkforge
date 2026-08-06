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

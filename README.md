# SwiftTrack - Airport Express & Rail Ticketing Application

SwiftTrack is a production-structured Android railway and airport-transfer ticketing application inspired by the workflow and convenience of airport rail booking systems. Built natively in **Java & XML** for Android, backed by a **Spring Boot 3 (Java 21)** REST API, **PostgreSQL**, **Redis**, and **Docker Compose**.

---

## Technical Stack

- **Android Client**: Java 17, XML Layouts, Material Components 3, MVVM Architecture, Room Database (Offline First), Retrofit 2, OkHttp 4, Navigation Component, ViewBinding, ZXing QR Generator, Android Keystore, WorkManager.
- **Backend**: Java 21, Spring Boot 3.2, Spring Security (JWT + Rotating Refresh Tokens), Spring Data JPA, PostgreSQL 16, Redis 7, Flyway Database Migrations, OpenAPI / Swagger UI.
- **Infrastructure**: Docker & Docker Compose.

---

## Quick Start & Setup

### 1. Launch Backend Infrastructure with Docker
```bash
docker-compose up --build
```
This spins up:
- **PostgreSQL Database** on `localhost:5432` with Flyway migrations & seed data automatically applied.
- **Redis Cache & Lock Server** on `localhost:6379`.
- **Spring Boot REST Backend** on `http://localhost:8080`.

OpenAPI documentation will be accessible at `http://localhost:8080/swagger-ui.html`.

---

### 2. Build & Run Android Application
1. Open the project directory in **Android Studio**.
2. Sync Gradle files (`app/build.gradle.kts`).
3. Launch an Android Emulator (API 34+).
4. Run `app`. The emulator will communicate with the backend at `http://10.0.2.2:8080/`.

---

## Test Accounts & Sandbox Credentials

- **Admin Account**: `admin@swifttrack.com` / `AdminPass123!`
- **Test Customer**: `testuser@swifttrack.com` / `AdminPass123!`
- **Sandbox Promo Code**: `EXPRESS20` (20% off)

---

## External Services & Cost Analysis

1. **Self-Hosted Infrastructure (PostgreSQL, Redis, Spring Boot)**: Free / Standard cloud compute cost (e.g. AWS EC2 t4g.medium or GCP e2-medium ~$15-$25/mo).
2. **Firebase Cloud Messaging (FCM)**: Free tier (no per-message cost).
3. **Stripe / Payment Gateway**: 1.4% + 20p per successful transaction in UK/EU (No upfront cost).

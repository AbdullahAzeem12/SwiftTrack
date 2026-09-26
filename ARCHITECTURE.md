# SwiftTrack System Architecture

```mermaid
graph TD
    subgraph Android Native Client (Java / XML)
        UI[Presentation Layer: ViewBinding Fragments & ViewModels]
        Domain[Domain Layer: Validation & Business Models]
        Data[Data Layer: Repositories & Room Database]
        Keystore[Android Keystore Security & EncryptedPrefs]

        UI --> Domain
        Domain --> Data
        Data --> Keystore
    end

    subgraph Spring Boot 3 Backend (Java 21)
        API[Spring MVC REST Controllers]
        Sec[Spring Security & JWT Filter]
        Services[Core Services & State Machine]
        JPA[Spring Data JPA Repositories]
        Flyway[Flyway Migrations]

        API --> Sec
        Sec --> Services
        Services --> JPA
        JPA --> Flyway
    end

    subgraph Infrastructure & Storage
        Postgres[(PostgreSQL 16 Relational DB)]
        Redis[(Redis 7 Locks & Cache)]
        FCM[Firebase Cloud Messaging]

        JPA --> Postgres
        Services --> Redis
        Services --> FCM
    end

    Data -->|HTTPS / JSON REST API| API
```

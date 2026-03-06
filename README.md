# BJJ Calendar

CLI application built with Spring Boot that aggregates Brazilian Jiu-Jitsu competition calendars from multiple federations and publishes them to a Teamup calendar.

## Supported Federations

- **IBJJF** — International Brazilian Jiu-Jitsu Federation
- **AJP** — Abu Dhabi Jiu-Jitsu Pro
- **CFJJB** — Confédération Française de Jiu-Jitsu Brésilien
- **Smoothcomp** — Grappling Industries, NAGA, and other Smoothcomp-hosted events

## Tech Stack

- Java 21
- Spring Boot 3.5.x (CLI via `CommandLineRunner`)
- Maven
- Jsoup (HTML scraping)
- OpenAPI Generator (Teamup API client)
- Lombok

## Architecture

Hexagonal architecture (Ports & Adapters):

```
com.onsayit.bjjcalendar
├── domain          # Event model, Federation enum, port interfaces
├── application     # Use cases and job runner
└── infrastructure  # Federation readers (API/scraping) and calendar writers
```

## Build

```bash
mvn compile    # Compile (includes OpenAPI generation)
mvn verify     # Full build with static analysis (Checkstyle, PMD, SpotBugs)
mvn test       # Run tests
```

## Configuration

The application requires API keys configured in `src/main/resources/application.yaml` to connect to federation APIs and the Teamup calendar.

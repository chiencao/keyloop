# Unified Service Scheduler

Backend for **Scenario A (Ownership)** — replaces manual dealership booking. A
customer requests a service appointment for a vehicle, service type, dealership
and time; the service verifies that **both a service bay and a qualified
technician** are free for the whole duration, then persists a **confirmed
appointment** linking customer, vehicle, technician and bay.

- **Design document:** [`docs/system-design.md`](docs/system-design.md) — architecture, data flow, tech justification, observability, and the GenAI-usage section (Part 1).
- **API contract:** [`docs/openapi.json`](docs/openapi.json) · live Swagger UI at `/swagger-ui.html`.
- **Stack:** Java 21 · Spring Boot 3.5 · Spring Data JPA · Flyway · H2 (dev) / PostgreSQL (prod) · Actuator + Micrometer.

## Requirements

- Java 21 (`java -version`)
- No local Maven needed — the Maven Wrapper (`./mvnw`) is included.

## Run

```bash
./mvnw spring-boot:run
```

Starts on `http://localhost:8080` with an embedded H2 file DB (`./data/`),
migrated and seeded by Flyway. Useful URLs:

- **Quick-Book front-end — `http://localhost:8080/`** (or just open
  `src/main/resources/static/index.html` in a browser — it's fully mocked and needs
  no backend). A service-first booking flow: service → nearest/usual dealership →
  open slot → confirm.
- Swagger UI — `http://localhost:8080/swagger-ui.html`
- Health — `http://localhost:8080/actuator/health`
- Metrics (Prometheus) — `http://localhost:8080/actuator/prometheus`

For production, run with PostgreSQL:

```bash
DB_URL=jdbc:postgresql://localhost:5432/scheduler DB_USERNAME=... DB_PASSWORD=... \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Test

```bash
./mvnw test
```

14 tests across the pyramid: **unit** (`BookingServiceTest`, Mockito), **web
slice** (`AppointmentControllerTest`, `@WebMvcTest`), and **integration**
(`SchedulerIntegrationTest`, real Flyway + H2) — including a **concurrency test
that proves no overbooking** (8 threads on a capacity-2 slot → exactly 2 confirmed).

## API

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/v1/appointments` | Book an appointment |
| `GET`  | `/api/v1/appointments/{id}` | Get an appointment |
| `GET`  | `/api/v1/appointments/availability` | Non-binding availability probe |
| `GET`  | `/api/v1/dealerships` · `/api/v1/service-types` · `/api/v1/vehicles` | Reference data |

### Try it (mocked client via cURL)

A ready-to-run script lives at [`docs/curl-examples.sh`](docs/curl-examples.sh).
Key calls:

```bash
# 1. Discover ids
curl -s localhost:8080/api/v1/dealerships
curl -s localhost:8080/api/v1/service-types

# 2. Check availability (non-binding)
curl -s "localhost:8080/api/v1/appointments/availability?dealershipId=1&serviceTypeId=1&desiredStart=2030-06-01T09:00:00"

# 3. Book (Oil Change, 30 min) -> 201 Created
curl -s -X POST localhost:8080/api/v1/appointments \
  -H 'Content-Type: application/json' \
  -d '{"dealershipId":1,"vehicleId":1,"serviceTypeId":1,"desiredStart":"2030-06-01T09:00:00"}'
```

Example `201` response:

```json
{
  "id": 1, "status": "CONFIRMED",
  "dealership": { "id": 1, "name": "Downtown Auto Service" },
  "customer":   { "id": 1, "name": "Alice Johnson" },
  "vehicle":    { "id": 1, "vin": "1HGCM82633A004352", "make": "Toyota", "model": "Corolla" },
  "technician": { "id": 1, "name": "Carlos Mendez" },
  "serviceBay": { "id": 1, "name": "Bay A" },
  "serviceType":{ "id": 1, "code": "OIL_CHANGE", "name": "Oil Change", "durationMinutes": 30 },
  "startTime": "2030-06-01T09:00:00", "endTime": "2030-06-01T09:30:00"
}
```

### Error responses

| Situation | Status | Body `message` |
|-----------|--------|----------------|
| Missing/invalid fields | `400` | `Validation failed` (+ `fieldErrors`) |
| Unknown dealership/vehicle/service type | `404` | `<Resource> not found: <id>` |
| No bay or qualified technician free | `409` | `No service bay available …` / `No qualified technician …` |
| Vehicle already booked in an overlapping window | `409` | `Vehicle <id> already has an appointment during …` |
| Outside dealership opening hours | `422` | `Requested window … is outside opening hours …` |

## Seed data (for demos/tests)

- **Dealership 1** “Downtown Auto Service” 08:00–18:00 — **3 GENERAL-capable
  technicians but only 2 bays**, so an Oil Change slot is capacity-limited to 2.
- Service types: `OIL_CHANGE` (GENERAL, 30m), `BRAKE_SERVICE` (BRAKES, 60m),
  `DIAGNOSTIC` (DIAGNOSTICS, 90m), `EV_SERVICE` (EV, 120m).
- Customer 1 Alice → Vehicle 1 (Toyota Corolla); Customer 2 Bob → Vehicle 2 (Tesla Model 3).

## Project layout

```
src/main/java/com/keyloop
├── domain/       JPA entities (Appointment, Technician, ServiceBay, ...)
├── repository/   Spring Data repos incl. overlap-availability queries + FOR UPDATE lock
├── service/      AvailabilityService (probe) · BookingService (@Transactional, race-safe)
├── web/          REST controllers + GlobalExceptionHandler
├── dto/          request/response records
└── config/       OpenAPI metadata
src/main/resources/static/index.html   Quick-Book front-end (mocked, standalone; also served at /)
src/main/resources/db/migration   Flyway V1 schema · V2 seed · V3 demo vehicles
docs/             system-design.md · system-design.html · openapi.json · curl-examples.sh
```

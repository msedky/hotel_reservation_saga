# Travel Booking — Saga Orchestration

A learning/portfolio project demonstrating **Saga Orchestration** for a travel agency that books flights and hotels through external providers, then processes payment. Built as four **independent Spring Boot Maven projects**, not a Maven multi-module project.

> **Status (14 October 2026):** Architecture is the **target design**, not a claim that all services and RabbitMQ integration are implemented. `flight-service` has its initial REST/controller, persistence and mock airline-provider code; `mvn clean compile` succeeded. Application startup reached Spring Boot but failed because the datasource URL was missing. A shared PostgreSQL 17 Docker container is now running; databases and datasource configuration are the next steps.

![Target architecture](architecture.png)

Editable diagram: [`architecture.mmd`](architecture.mmd) (Mermaid). Vector image: [`architecture.svg`](architecture.svg).

## 1. Services and responsibilities

| Service | Local port (proposed) | Database | Responsibilities |
|---|---:|---|---|
| `booking-service` | `8081` | `booking_db` | Public booking API; orchestrates saga; persists booking/saga progress; sends commands and handles replies |
| `flight-service` | `8082` | `flight_db` | Reserves/cancels flights via an airline provider client; stores local reservation and external reference |
| `hotel-service` | `8083` | `hotel_db` | Reserves/cancels hotels via a provider client; stores local reservation and external reference |
| `payment-service` | `8084` | `payment_db` | Charges/refunds via a payment provider; stores transaction state and idempotency data |

**Important domain boundary:** This is a *travel agency*, not an airline or hotel operator. The flight service does **not** own airline seat inventory. The same external-provider approach is intended for hotel reservations.

## 2. Technology choices

- **Java 21** (source/release target). Ensure Maven's runtime JDK is also 21.
- **Spring Boot 4.1.1** in the supplied `flight-service/pom.xml` (not necessarily the version of other services).
- Spring Web MVC, Spring Data JPA, Jakarta Validation, Lombok.
- **RabbitMQ** for asynchronous saga commands, results, and compensations (**planned**).
- **PostgreSQL 17**, Flyway migrations; **database per service**, sharing one local Docker PostgreSQL instance.
- **Mock external provider clients** for deterministic development; replaceable with HTTP integrations later.
- Testcontainers and automated tests (**planned/partly scaffolded**, not yet verified end-to-end).

PostgreSQL is a pragmatic choice for transactional booking/payment data. **MongoDB is not part of the initial implementation**; a future booking-history/read-model projection could use MongoDB to demonstrate CQRS and eventual consistency if justified.

## 3. Target saga flow

### Success

```text
Client -> booking-service: Create booking
booking-service -> RabbitMQ: ReserveFlight command
RabbitMQ -> flight-service: ReserveFlight
flight-service -> Airline Provider (mock): bookFlight
flight-service -> RabbitMQ: FlightReserved
booking-service -> RabbitMQ: ReserveHotel command
RabbitMQ -> hotel-service: ReserveHotel
hotel-service -> RabbitMQ: HotelReserved
booking-service -> RabbitMQ: ChargePayment command
RabbitMQ -> payment-service: ChargePayment
payment-service -> RabbitMQ: PaymentSucceeded
booking-service: Booking CONFIRMED
```

### Failure / compensation

```text
If hotel reservation fails:
  -> CancelFlight -> Booking FAILED (once compensation confirmed)

If payment fails after both reservations succeed:
  -> CancelHotel -> CancelFlight -> Booking FAILED
```

The orchestrator must track each step and compensation outcome; failures or timeouts during compensation need retry/recovery rather than silently marking the saga complete.

## 4. Local PostgreSQL setup

A reusable PostgreSQL 17 container was created with:

```powershell
docker run -d `
  --name postgres-local `
  --restart unless-stopped `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=postgres123 `
  -e POSTGRES_DB=postgres `
  -p 5432:5432 `
  -v postgres-data:/var/lib/postgresql/data `
  postgres:17
```

> This is a **local development password**. Do not commit production credentials. The container already exists; **do not rerun** `docker run` with the same name. Use `docker start postgres-local` if it is stopped.

Create the four databases:

```powershell
docker exec -it postgres-local psql -U postgres
```

```sql
CREATE DATABASE booking_db;
CREATE DATABASE flight_db;
CREATE DATABASE hotel_db;
CREATE DATABASE payment_db;
```

In `psql`, `\l` lists databases; `\q` exits.

For `flight-service/src/main/resources/application.yml` (local example):

```yaml
spring:
  application:
    name: flight-service
  datasource:
    url: jdbc:postgresql://localhost:5432/flight_db
    username: postgres
    password: ${DB_PASSWORD:postgres123}
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
server:
  port: 8081
```

Check for an existing `application.properties`/`application.yml` and existing Flyway migrations before replacing files. Flyway should create the reservation table, provided the migration exists and matches the entity.

## 5. Flight service: current REST interface

These endpoints were added to the initial `flight-service` implementation; they are **direct development/test endpoints**, not the final RabbitMQ-driven saga interface.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/flight-reservations` | Reserve flight |
| `GET` | `/api/v1/flight-reservations/by-booking/{bookingId}` | Read reservation |
| `POST` | `/api/v1/flight-reservations/by-booking/{bookingId}/cancel` | Cancel reservation |

Example request:

```json
{
  "bookingId": "b1d2f40e-9f9c-4a39-8c58-0c663d54f71b",
  "sagaId": "c2c3e50f-a0ad-4b49-9d69-1d774e65a82c",
  "flightOfferId": "FLIGHT-OFFER-001"
}
```

`flightOfferId = "UNAVAILABLE"` triggers the mock airline provider's failure scenario. The initial `FlightReservationStatus` values are `PENDING`, `CONFIRMED`, `FAILED`, `CANCELLED`.

**Initial implementation limitations:** Idempotency is only a basic lookup by `bookingId`, not concurrency-safe. A provider/network failure can leave a `PENDING` reservation; recovery and retries are not yet implemented. Provider cancellation is a no-op in the mock. Do not treat these as production-ready semantics.

## 6. Reliability topics to implement deliberately

1. Saga persistence with explicit step and compensation state transitions.
2. RabbitMQ exchanges, queues, routing keys and command/result contracts (including `sagaId`, `bookingId`, correlation/message IDs).
3. Idempotent consumers and deduplication, including concurrency tests.
4. Transactional outbox (publish reliably after local DB commit).
5. Retry/backoff, dead-letter queues, timeouts, recovery of interrupted sagas.
6. Compensation failures, duplicate replies, out-of-order events, and provider uncertainty.
7. Integration tests using PostgreSQL/RabbitMQ Testcontainers.
8. Observability: structured logs, correlation IDs, metrics and tracing.

## 7. Immediate next steps

1. Create `booking_db`, `flight_db`, `hotel_db`, `payment_db` in the existing `postgres-local` container.
2. Add the `flight-service` datasource config; verify Flyway migration and run `mvn spring-boot:run`.
3. Test flight reservation and cancellation via Postman.
4. Implement the **first vertical slice**: `booking-service -> RabbitMQ -> flight-service -> RabbitMQ -> booking-service`.
5. Add hotel, then payment, then compensation and failure/retry paths.

## 8. Context for continuing in a new chat

Copy/paste the following:

> We are building `Hotel_Reservation_Saga`, a Java 21 travel-agency Saga Orchestration portfolio project. It has four independent Spring Boot Maven projects: `booking-service` (orchestrator), `flight-service`, `hotel-service`, `payment-service`. RabbitMQ will carry commands, replies, and compensations. Each service owns a PostgreSQL database (`booking_db`, `flight_db`, `hotel_db`, `payment_db`) on a shared local Docker PostgreSQL 17 container named `postgres-local` exposed at localhost:5432 (local username postgres; configure password locally). The flight-service uses Spring Boot 4.1.1 and currently has a FlightReservation entity, repository, service, controller, mock AirlineProviderClient, and Flyway migration. Its REST endpoints are POST `/api/v1/flight-reservations`, GET `/api/v1/flight-reservations/by-booking/{bookingId}`, and POST `/api/v1/flight-reservations/by-booking/{bookingId}/cancel`. `mvn clean compile` passed targeting Java 21; Spring Boot startup failed due to missing datasource URL, so configure DB and retest. Maven ran with Java 24 previously despite release 21; align runtime to Java 21. Travel agency services use external providers, not internal airline/hotel inventory. First finish flight-service local run and Postman checks, then implement Booking → RabbitMQ → Flight → RabbitMQ → Booking. Avoid claiming planned features are already implemented; prioritize code-first steps and keep scope focused.

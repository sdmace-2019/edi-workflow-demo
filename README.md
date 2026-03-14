# EDI Workflow Demo

A full-stack healthcare EDI (Electronic Data Interchange) claims processing system built as a portfolio demonstration. It simulates a real-world payer/provider data pipeline: ingesting raw X12 837 (claim) and 835 (ERA/payment) files, parsing them into structured records, running validation rules, and surfacing results through a REST API and Vue dashboard.

---

## What This Demonstrates

This project is intentionally scoped to show depth across several layers of a production-style backend system:

- **EDI domain knowledge** — X12 837/835 segment parsing, loop structure, transaction classification
- **Spring Boot architecture** — layered package design (controllers → services → parsers → repositories), `@ConfigurationProperties`, `@ControllerAdvice`, pagination, CORS
- **Data modeling** — JPA entities mapped to PostgreSQL, manual schema migration, Hibernate validation-only mode (`ddl-auto: validate`)
- **Business rules engine** — pluggable validation rules with idempotent re-validation and per-claim audit logs
- **Testing discipline** — unit tests (plain Java, no Spring), `@WebMvcTest` slice tests (mocked services, no DB), and `@SpringBootTest` integration tests against a real PostgreSQL database (68 tests, 0 failures)
- **Vue 3 frontend** — router-based SPA consuming the REST API, no UI framework dependency

---

## Tech Stack

| Layer    | Technology                      |
| -------- | ------------------------------- |
| Backend  | Java 17, Spring Boot 3.5, Maven |
| ORM      | Spring Data JPA / Hibernate     |
| Database | PostgreSQL 15+                  |
| Frontend | Vue 3, Vue Router 4, Vite       |
| Testing  | JUnit 5, Mockito, AssertJ       |
| Models   | Lombok                          |

---

## Project Structure

```
edi-workflow-demo/
├── backend/                    Spring Boot REST API
│   ├── src/main/java/com/edi/backend/
│   │   ├── config/             CorsConfig, EdiIntakeProperties
│   │   ├── controllers/        REST endpoints + GlobalExceptionHandler
│   │   ├── models/             JPA entities (Claim, Payment, ValidationLog)
│   │   ├── parsers/            Edi837Parser, Edi835Parser, EdiTransactionClassifier
│   │   ├── repositories/       Spring Data JPA interfaces
│   │   └── services/           Business logic (ingest, validation)
│   └── src/test/               68 tests across unit, WebMvc, and integration layers
├── database/migrations/        SQL migration scripts (run manually in order)
│   ├── 01_create_claims.sql
│   ├── 02_create_payments.sql
│   ├── 03_create_validation_logs.sql
│   └── 04_create_views.sql
├── edi-samples/                Sample X12 EDI files used for demo ingest
│   ├── sample_837.edi          Two valid claims + one intentional duplicate
│   └── sample_835.edi          ERA with two CLP payments and a PLB provider adjustment
└── frontend/                   Vue 3 SPA
    └── src/
        ├── api/index.js        Centralized fetch client (no hardcoded backend URL)
        ├── router/index.js     Client-side routing
        └── views/              Dashboard, Claims list/detail, Payments
```

---

## Prerequisites

| Requirement | Version  |
| ----------- | -------- |
| Java JDK    | 17 or 21 |
| Maven       | 3.8+     |
| PostgreSQL  | 15+      |
| Node.js     | 18+      |
| npm         | 9+       |

---

## Setup

### 1. PostgreSQL — Create the database and user

```sql
CREATE DATABASE edi_demo;
CREATE USER edi_user WITH PASSWORD 'edi_password123';
GRANT ALL PRIVILEGES ON DATABASE edi_demo TO edi_user;
\c edi_demo
GRANT ALL ON SCHEMA public TO edi_user;
```

### 2. Run the migrations (in order)

```powershell
psql -U edi_user -d edi_demo -f database/migrations/01_create_claims.sql
psql -U edi_user -d edi_demo -f database/migrations/02_create_payments.sql
psql -U edi_user -d edi_demo -f database/migrations/03_create_validation_logs.sql
psql -U edi_user -d edi_demo -f database/migrations/04_create_views.sql
```

> **Note:** The schema is managed manually. `ddl-auto: validate` is set intentionally — Hibernate will validate the schema on startup but never alter it.

### 3. Start the backend

```bash
cd backend
mvn -DskipTests spring-boot:run
```

The API starts on **http://localhost:8080**.

> To verify: `curl http://localhost:8080/api/claims` should return an empty page JSON object.

### 4. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

The app opens on **http://localhost:3000**. All `/api` requests are proxied to the backend — no CORS issues in development.

---

## Loading Sample Data

Once both servers are running, click **"Load Sample EDI"** on the dashboard, or call the endpoint directly:

```bash
curl -X POST http://localhost:8080/api/intake/samples/ingest
```

This will:

1. Scan `edi-samples/` for `.edi` files
2. Classify each file as 837 or 835
3. Parse and persist **2 claims** (1 duplicate in the sample file is intentionally skipped) and **2 payments**
4. Return a JSON summary of saved/skipped counts

Re-running ingest is idempotent — duplicate claims and payments are detected and skipped.

---

## Running Validation

After ingesting, validate all claims:

```bash
curl -X POST http://localhost:8080/api/claims/validate-all
```

Each claim is checked against 6 rules:

| Rule                      | Description                                     |
| ------------------------- | ----------------------------------------------- |
| `BILLED_AMOUNT_POSITIVE`  | Billed amount must be > 0                       |
| `SERVICE_DATE_NOT_FUTURE` | Service date cannot be in the future            |
| `PROCEDURE_CODE_FORMAT`   | Must match CPT/HCPCS format (`^[A-Z0-9]{4,7}$`) |
| `DIAGNOSIS_CODE_FORMAT`   | Accepts ICD-10 with or without dot separator    |
| `PAYER_ID_PRESENT`        | Payer ID must be non-blank                      |
| `PROVIDER_ID_PRESENT`     | Provider NPI must be non-blank                  |

Validation is **idempotent** — re-running clears prior results and re-evaluates fresh.

---

## API Reference

| Method | Endpoint                            | Description                          |
| ------ | ----------------------------------- | ------------------------------------ |
| POST   | `/api/intake/samples`               | Scan sample folder, return file list |
| POST   | `/api/intake/samples/ingest`        | Parse + save all EDI files           |
| GET    | `/api/claims?page=0&size=20`        | Paginated claim list                 |
| GET    | `/api/claims/{claimId}`             | Single claim by ID                   |
| GET    | `/api/claims/{claimId}/validations` | Validation results for a claim       |
| POST   | `/api/claims/{claimId}/validate`    | Validate a single claim              |
| POST   | `/api/claims/validate-all`          | Validate all claims                  |
| GET    | `/api/claims/summary`               | Status counts + billed/paid totals   |
| GET    | `/api/payments`                     | List all payments                    |
| GET    | `/api/payments/{paymentId}`         | Single payment by ID                 |
| GET    | `/api/dashboard`                    | Combined summary for dashboard       |

---

## Running Tests

```bash
cd backend
mvn test
```

**68 tests, 0 failures** across three test layers:

| Layer                 | Class                           | Tests | Pattern                                 |
| --------------------- | ------------------------------- | ----- | --------------------------------------- |
| Unit                  | `Edi837ParserTest`              | 16    | Plain Java, no Spring context           |
| Unit                  | `Edi835ParserTest`              | 16    | Plain Java, no Spring context           |
| Unit                  | `ValidationServiceTest`         | 21    | Mockito mocks, no DB                    |
| Web layer (no DB)     | `IntakeAndClaimsControllerTest` | 6     | `@WebMvcTest` + `@MockitoBean` services |
| Integration (real DB) | `IngestIntegrationTest`         | 8     | `@SpringBootTest` + live PostgreSQL     |

The integration tests require PostgreSQL to be running with the `edi_demo` database and migrations applied.

---

## Design Decisions Worth Noting

**No foreign key from payments to claims.** In real-world healthcare EDI, 835 ERA remittance files are transmitted by payers independently of and often before the corresponding 837 claims are entered into a system. Enforcing a DB-level FK would cause valid payments to be rejected. The `claim_id` on the `payments` table is a plain `VARCHAR` used for reconciliation queries only.

**Schema-first, not ORM-first.** Hibernate is set to `validate` mode — the schema is owned by the SQL migration files, not generated by the ORM. This reflects how production healthcare systems operate, where DBAs control schema changes.

**Idempotent ingest and validation.** Both the ingest pipeline and the validation engine are safe to re-run. Duplicate claims and payments are detected by unique ID before insert. Validation deletes prior logs for a claim before re-evaluating, ensuring audit results are always current.

---

## Author

Shawn Mace — Healthcare IT specialist with 4 years building production EDI systems for high-volume revenue cycle operations, and 3 years of prior clinical experience as a Certified Medical Assistant. I build tooling based on real-world needs.

[LinkedIn](https://www.linkedin.com/in/sdm1110) | [GitHub](https://github.com/sdmace-2019)

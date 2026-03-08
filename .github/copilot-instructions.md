# GitHub Copilot Instructions — EDI Workflow Demo

## Project Overview

This is a Java/Spring Boot + Vue portfolio project that simulates a healthcare
EDI workflow. It parses X12 837 (claims) and 835 (payments/ERA) files, persists
data to PostgreSQL, runs validation rules, and exposes a REST API.

---

## Non-Negotiable Rules

### 1. Package Declarations Must Match Directory Path Exactly

Every `.java` file MUST have its package declaration match its directory:

```
src/main/java/com/edi/backend/controllers/  → package com.edi.backend.controllers;
src/main/java/com/edi/backend/services/     → package com.edi.backend.services;
src/main/java/com/edi/backend/parsers/      → package com.edi.backend.parsers;
src/main/java/com/edi/backend/repositories/ → package com.edi.backend.repositories;
src/main/java/com/edi/backend/models/       → package com.edi.backend.models;
src/main/java/com/edi/backend/config/       → package com.edi.backend.config;
```

Mismatched packages cause "file does not contain class" compile errors.

### 2. Always Use Lombok on Entity Classes

All JPA entity classes use:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
```

- **Never** write getters, setters, or constructors manually on entity classes
- Lombok must be present in `pom.xml` as `<optional>true</optional>`
- If getters are "not found" at compile time, check Lombok is in `pom.xml`

### 3. Schema Is Managed by SQL Migrations — Never by Hibernate

- `spring.jpa.hibernate.ddl-auto: validate` is set in `application.yml`
- Never change this to `create`, `create-drop`, or `update`
- All DB changes go in `database/migrations/` as numbered SQL files
- Java field names in entities MUST match the DB column names in migrations

### 4. Configuration Properties Pattern

- Use `@ConfigurationProperties(prefix = "...")` for config classes
- Register with `@EnableConfigurationProperties(XxxProperties.class)` in
  `BackendApplication.java` — not `@ConfigurationPropertiesScan`
- Config values go in `src/main/resources/application.yml`

### 5. Dependency Build Order

When creating new features, always build in this order:

1. SQL migration (already applied to DB)
2. Model/Entity `.java`
3. Repository `.java`
4. Parser `.java` (if needed)
5. Service `.java`
6. Controller `.java`

Skipping steps causes cascade compile failures.

---

## Architecture Conventions

### Layer Responsibilities

| Layer      | Annotation                 | Responsibility                                       |
| ---------- | -------------------------- | ---------------------------------------------------- |
| Controller | `@RestController`          | HTTP mapping only, no business logic                 |
| Service    | `@Service`                 | Business logic, file I/O, orchestration              |
| Parser     | `@Component`               | Stateless EDI parsing, one class per transaction set |
| Repository | interface                  | Spring Data JPA only, no implementation needed       |
| Model      | `@Entity`                  | JPA entity + Lombok, no business logic               |
| Config     | `@ConfigurationProperties` | Typed config binding only                            |

### Naming Conventions

| Type           | Pattern              | Example                        |
| -------------- | -------------------- | ------------------------------ |
| Controller     | `XxxController.java` | `ClaimsController.java`        |
| Service        | `XxxService.java`    | `SampleEdiIngestService.java`  |
| Parser         | `EdiXxxParser.java`  | `Edi837Parser.java`            |
| Repository     | `XxxRepository.java` | `ClaimRepository.java`         |
| Model          | singular noun        | `Claim.java`, `Payment.java`   |
| REST base path | `/api/xxx` (plural)  | `/api/claims`, `/api/payments` |

### REST Endpoint Conventions

- `GET /api/claims` — list all
- `GET /api/claims/{claimId}` — get by business key (not DB id)
- `POST /api/intake/samples` — scan sample folder
- `POST /api/intake/samples/ingest` — parse and persist EDI files
- Return `ResponseStatusException` with appropriate `HttpStatus` on errors

---

## EDI Parsing Rules

### File Format

- Segment delimiter: `~`
- Element delimiter: `*`
- Component delimiter: `:`
- Split segments: `ediText.split("~\\s*")`
- Split elements: `seg.split("\\*", -1)` (use `-1` to preserve trailing empty elements)

### Transaction Set Detection

- Read `ST*xxx` segment — element index 1 is the transaction set ID
- `837` = claim, `835` = payment/ERA
- Handled by `EdiTransactionClassifier.detectTransactionSetId(String ediText)`

### 837 Parser Rules

- `CLM` segment starts a new claim
- `NM1*IL` and `NM1*QC` both map to `patientId` (subscriber = patient for demo)
- `NM1*82` = rendering provider → `providerId`
- `NM1*PR` = payer → `payerId`
- Loop-level NM1s (before CLM) are captured and applied to the next CLM
- `DTP*472` = service date (format `D8*YYYYMMDD`)
- `SV1*HC:99213` → procedure code is the part after `:`
- `HI*ABK:J0690` → diagnosis code is the part after `:`

### 835 Parser Rules (to be implemented)

- `TRN*1*ERA-ID` → ERA reference ID (TRN02)
- `BPR*I*950.00` → total payment amount (BPR02)
- `CLP*CLM-ID*1*billed*paid` → claim-level payment (one Payment row per CLP)
- `DTM*405*YYYYMMDD` → payment date
- `NM1*QC` → patient, `NM1*82` → provider
- `PLB` → provider-level adjustment

### Ingest Guard Rules (SampleEdiIngestService)

A claim is skipped (not errored) if ANY required field is null/blank:
`claimId`, `patientId`, `providerId`, `payerId`, `serviceDate`,
`procedureCode`, `diagnosisCode`, `billedAmount`

De-duplication: `existsByClaimId()` before every save.

---

## Database

### Connection

- Database: `edi_demo`
- User: `edi_user`
- Configured in `application.yml`

### Tables

| Table             | Primary Entity       | Key Business Column           |
| ----------------- | -------------------- | ----------------------------- |
| `claims`          | `Claim.java`         | `claim_id` (UNIQUE, NOT NULL) |
| `payments`        | `Payment.java`       | not yet created               |
| `validation_logs` | `ValidationLog.java` | not yet created               |

### Claim Entity Defaults (set in `@PrePersist`)

- `claimStatus` defaults to `"RECEIVED"`
- `ediType` defaults to `"837"`
- `createdAt` and `updatedAt` set automatically

---

## Common Diagnostic Commands (Windows PowerShell)

```powershell
# Compile and capture errors
mvn -DskipTests clean compile 2>&1 | Tee-Object .\compile-errors.txt
Select-String -Path .\compile-errors.txt -Pattern "\[ERROR\]"

# Run app
cd C:\Users\shawn\OneDrive\Desktop\resume\edi-workflow-demo\backend
mvn -DskipTests spring-boot:run

# Test endpoints
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/intake/samples/ingest
$r = Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/claims
$r | ConvertTo-Json -Depth 5

# Check registered Spring mappings
Invoke-WebRequest http://localhost:8080/actuator/mappings -OutFile .\mappings-raw.json
Select-String -Path .\mappings-raw.json -Pattern "/api/" -CaseSensitive:$false

# Verify a .class file was produced
Test-Path ".\target\classes\com\edi\backend\services\SampleEdiIngestService.class"

# DB query
psql -U edi_user -d edi_demo -c "SELECT id, claim_id, claim_status FROM claims;"
```

---

## What NOT to Do

- ❌ Do not write getters/setters manually on `@Data` Lombok classes
- ❌ Do not change `ddl-auto` to anything other than `validate`
- ❌ Do not put business logic in controllers
- ❌ Do not create a new service before its dependent model and repository exist
- ❌ Do not use `@ConfigurationPropertiesScan` — use `@EnableConfigurationProperties`
- ❌ Do not assume a Spring bean loaded — always verify with `/actuator/mappings`
  if an endpoint returns 404

### Known Issue 🐛 → Resolved ✅

- sample_837.edi contains 3 CLM segments; the 3rd is an intentional duplicate
- De-duplication via existsByClaimId() silently skips it — working as designed
- Demonstrates real-world duplicate 837 handling

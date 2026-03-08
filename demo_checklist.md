User: Create a checklist that we are following to complete this task. I reference material to ensure that we are not getting ahead of ourselves. Example: I already have ClaimRepository.java fill created.

GitHub Copilot: ## EDI Workflow Demo — Build Checklist

### Stage 1: Project Setup ✅

- [x] Create repo edi-workflow-demo
- [x] Initialize Spring Boot backend (`com.edi.backend`)
- [x] Configure `application.yml` (DB connection, JPA, logging)
- [x] Add `pom.xml` with Spring Boot Maven plugin

### Stage 2: Database Setup ✅

- [x] Create PostgreSQL database `edi_demo`
- [x] Create `edi_user` with permissions on `public` schema
- [x] Migration `01_create_claims.sql` (BIGSERIAL id)
- [x] Migration `02_create_payments.sql`
- [x] Migration `03_create_validation_logs.sql`
- [x] Migration `04_create_views.sql`
- [x] JPA schema validation passes (`ddl-auto: validate`)

### Stage 3: Core Models ✅

- [x] `Claim.java` entity (mapped to `claims` table)
- [x] `Payment.java` entity (mapped to `payments` table)
- [x] `ValidationLog.java` entity (mapped to `validation_logs` table)

### Stage 4A: Sample Folder Intake (Scan) ✅

- [x] `EdiIntakeProperties.java` (`@ConfigurationProperties`)
- [x] `@EnableConfigurationProperties` in `BackendApplication.java`
- [x] `edi.intake.sample-dir` set in `application.yml`
- [x] `SampleFolderIntakeService.java` (scans folder, lists files)
- [x] `IntakeController.java` — `POST /api/intake/samples`

### Stage 4B: Sample Folder Ingest (Parse + Save) ✅

- [x] `ClaimRepository.java` — `findByClaimId`, `existsByClaimId`
- [x] `EdiTransactionClassifier.java` — detects `ST*837` vs `ST*835`
- [x] `Edi837Parser.java` — parses CLM, NM1, DTP, SV1, HI segments
- [x] `SampleEdiIngestService.java` — reads folder, calls parser, saves claims
- [x] `IntakeController.java` — add `POST /api/intake/samples/ingest`
- [x] Verify `POST /api/intake/samples/ingest` returns claims saved/skipped
- [x] Verify rows appear in `claims` table via `GET /api/claims`
- [x] Verify rows in DB via `psql` query

### Stage 4C: Read-Back Endpoint ✅

- [x] `ClaimsController.java` — `GET /api/claims`
- [x] `ClaimsController.java` — `GET /api/claims/{claimId}`

### Stage 5: 835 Payment Parsing ✅

- [x] `Edi835Parser.java` — parses BPR, DTM, N1, CLP, CAS, PLB segments
- [x] `PaymentRepository.java` — `existsByPaymentId()` for de-duplication
- [x] `Payment.java` — fixed column mapping to match `payments` table exactly
- [x] `SampleEdiIngestService.java` — wired in 835 parser, saves payments
- [x] Verify 2 payments saved to `payments` table
- [x] Verify de-duplication works (re-ingest skips duplicates)
- [x] Note: 3rd CLM in sample_837.edi is intentional duplicate — skipped by design

### Stage 6: Validation Engine ✅

- [x] `ValidationLog.java` — JPA entity mapped to `validation_logs`
- [x] `ValidationLogRepository.java` — `findByClaimId`, `deleteByClaimId`
- [x] `ValidationService.java` — 6 rules, idempotent re-validation
  - [x] `BILLED_AMOUNT_POSITIVE`
  - [x] `SERVICE_DATE_NOT_FUTURE`
  - [x] `PROCEDURE_CODE_FORMAT`
  - [x] `DIAGNOSIS_CODE_FORMAT` (accepts EDI format, dot optional)
  - [x] `PAYER_ID_PRESENT`
  - [x] `PROVIDER_ID_PRESENT`
- [x] `GET /api/claims/{claimId}/validations`
- [x] `POST /api/claims/{claimId}/validate`
- [x] `POST /api/claims/validate-all`
- [x] Verify 12 validation logs in DB (2 claims × 6 rules), all PASS

### Stage 7: REST API Polish ✅

- [x] `GET /api/claims/summary` — counts by status, total billed, total paid
- [x] `GET /api/payments` — list all payments
- [x] `GET /api/payments/{paymentId}` — single payment by `payment_id`
- [x] `GET /api/dashboard` — combined claims + payments + validation summary
- [x] CORS configuration for Vue frontend (`localhost:3000`)
- [x] Consistent error responses (`@ControllerAdvice`)
- [x] Pagination on `GET /api/claims`

### Stage 8: Frontend (Vue) ← YOU ARE HERE

- [ ] Scaffold Vue app
- [ ] Claims list page
- [ ] Claim detail page (with validation results)
- [ ] Payments list page
- [ ] Intake trigger button ("Load Sample EDI")

### Stage 9: Testing

- [ ] Unit tests for `Edi837Parser`
- [ ] Unit tests for `Edi835Parser`
- [ ] Unit tests for `ValidationService`
- [ ] Integration test for `POST /api/intake/samples/ingest`
- [ ] Integration test for `GET /api/claims`

### Stage 10: Demo Readiness

- [ ] README with setup instructions
- [ ] Sample EDI files committed to `edi-samples/`
- [ ] Git history clean with meaningful commit messages
- [ ] App runs end-to-end from `mvn spring-boot:run`

---

**Current focus:** Stage 7 — REST API Polish

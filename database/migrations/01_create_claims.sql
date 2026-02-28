-- Claims table: stores parsed 837 claim data
CREATE TABLE claims (
    id                  BIGSERIAL PRIMARY KEY,
    claim_id            VARCHAR(50) UNIQUE NOT NULL,      -- ICN / claim control number
    patient_id          VARCHAR(50) NOT NULL,              -- anonymized patient identifier
    provider_id         VARCHAR(50) NOT NULL,              -- rendering provider NPI
    payer_id            VARCHAR(50) NOT NULL,              -- insurance payer ID
    service_date        DATE NOT NULL,                     -- date of service
    procedure_code      VARCHAR(10) NOT NULL,              -- CPT/HCPCS code
    diagnosis_code      VARCHAR(15) NOT NULL,              -- ICD-10 code
    billed_amount       NUMERIC(12,2) NOT NULL,            -- total billed charges
    claim_status        VARCHAR(20) DEFAULT 'RECEIVED',    -- RECEIVED | VALIDATED | DUPLICATE | PROCESSED | REJECTED
    edi_type            VARCHAR(10) DEFAULT '837',         -- 837P | 837I | 837D
    raw_edi_reference   TEXT,                              -- reference to source EDI file
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);

-- Index for duplicate detection queries
CREATE INDEX idx_claims_duplicate_check
    ON claims (patient_id, provider_id, service_date, procedure_code, billed_amount);

-- Index for payer/provider reporting
CREATE INDEX idx_claims_payer_provider
    ON claims (payer_id, provider_id, claim_status);
CREATE TABLE validation_logs (
    id              SERIAL PRIMARY KEY,
    claim_id        VARCHAR(50) REFERENCES claims(claim_id),
    validation_type VARCHAR(50) NOT NULL,     -- DUPLICATE | STRUCTURE | ELIGIBILITY | BALANCE
    status          VARCHAR(20) NOT NULL,     -- PASS | FAIL | WARNING
    message         TEXT,                    -- human-readable result detail
    created_at      TIMESTAMP DEFAULT NOW()
);

-- Index for claim-level validation history lookup
CREATE INDEX idx_validation_claim_id
    ON validation_logs (claim_id, validation_type);
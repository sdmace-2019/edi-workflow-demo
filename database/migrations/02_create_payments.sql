CREATE TABLE payments (
    id                  SERIAL PRIMARY KEY,
    payment_id          VARCHAR(50) UNIQUE NOT NULL,      -- unique payment identifier
    claim_id            VARCHAR(50),                       -- ERA claim ref; NO FK — 835s arrive independently of 837s
    provider_id         VARCHAR(50) NOT NULL,              -- pay-to prov NPI
    payer_id            VARCHAR(50) NOT NULL,              -- sending payer
    payment_amount      NUMERIC(12,2) NOT NULL,            -- total payment amount
    adj_amount          NUMERIC(12,2) NOT NULL,            -- total adjustment amount
    plb_amount          NUMERIC(12,2) NOT NULL,            -- total PLB amount
    payment_date        DATE NOT NULL,                     -- date of payment
    payment_method      VARCHAR(20) NOT NULL,              -- e.g. EFT, check
    era_status          VARCHAR(20) DEFAULT 'RECEIVED',    -- RECEIVED | VALIDATED | PROCESSED | REJECTED | POSTED
    ctime               TIMESTAMP DEFAULT NOW(),
    update_time         TIMESTAMP DEFAULT NOW()
);

-- Idx for ERA reconciliation lookups
CREATE INDEX idx_payments_claim_id
    ON payments (claim_id); 

-- Idx for provider payment tracking and reporting
CREATE INDEX idx_payments_provider_payer
    ON payments (provider_id);

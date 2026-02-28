CREATE TABLE payments (
    id                  BIGSERIAL PRIMARY KEY,
    payment_id          VARCHAR(50) UNIQUE NOT NULL,      -- unique payment identifier
    claim_id            VARCHAR(50) REFERENCES claims(claim_id),              
    provider_id         VARCHAR(50) NOT NULL,              -- pay-to prov NPI
    payer_id            VARCHAR(50) NOT NULL,              -- sending payer
    payment_amount      NUMERIC(12,2) NOT NULL,            -- total payment amount
    adj_amount          NUMERIC(12,2) NOT NULL,            -- total adjustment amount
    plb_amount          NUMERIC(12,2) NOT NULL,            -- total PLB amount
    payment_date        DATE NOT NULL,                     -- date of payment
    payment_method      VARCHAR(20) NOT NULL,              -- e.g. EFT, check
    era_status          VARCHAR(20) DEFAULT 'RECEIVED',    -- RECEIVED | VALIDATED | PROCESSED | REJECTED | POSTED
    ctime               TIMESTAMP DEFAULT NOW(),
    update_time         TIMESTAMP DEFAULT NOW(),

    FOREIGN KEY (claim_id) REFERENCES claims(claim_id)    
);

-- Idx for ERA reconciliation lookups
CREATE INDEX idx_payments_claim_id
    ON payments (claim_id); 

-- Idx for provider payment tracking and reporting
CREATE INDEX idx_payments_provider_payer
    ON payments (provider_id);

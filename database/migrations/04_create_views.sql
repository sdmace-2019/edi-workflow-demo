CREATE VIEW claims_summary AS 
SELECT 
    c.claim_id,
    c.patient_id,
    c.provider_id,
    c.payer_id,
    c.service_date,
    c.procedure_code,
    c.billed_amount,
    c.claim_status,
    p.payment_amount,
    p.adj_amount,
    p.plb_amount,
    p.era_status,
    COUNT(v.id) AS validation_events,
    SUM(
        CASE
        WHEN v.status = 'FAIL'
        THEN 1
        ELSE 0
        END
    ) AS validation_failures
FROM claims c
LEFT JOIN payments p ON c.claim_id = p.claim_id
LEFT JOIN validation_logs v ON c.claim_id = v.claim_id
GROUP BY c.claim_id,c.patient_id,c.provider_id,c.payer_id,c.service_date,c.procedure_code,c.billed_amount,c.claim_status,p.payment_amount,p.adj_amount,p.plb_amount,p.era_status;
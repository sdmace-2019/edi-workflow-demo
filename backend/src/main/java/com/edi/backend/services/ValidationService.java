package com.edi.backend.services;

import com.edi.backend.models.Claim;
import com.edi.backend.models.ValidationLog;
import com.edi.backend.repositories.ClaimRepository;
import com.edi.backend.repositories.ValidationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ValidationService {

    private final ClaimRepository claimRepository;
    private final ValidationLogRepository validationLogRepository;

    public ValidationService(ClaimRepository claimRepository,
                             ValidationLogRepository validationLogRepository) {
        this.claimRepository = claimRepository;
        this.validationLogRepository = validationLogRepository;
    }

    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //

    /** Validate a single claim by its business claimId. */
    @Transactional
    public List<ValidationLog> validateClaim(String claimId) {
        Claim claim = claimRepository.findByClaimId(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        return runAndPersist(claim);
    }

    /** Validate every claim in the database. Returns total logs written. */
    @Transactional
    public int validateAll() {
        List<Claim> claims = claimRepository.findAll();
        int total = 0;
        for (Claim claim : claims) {
            total += runAndPersist(claim).size();
        }
        return total;
    }

    // ------------------------------------------------------------------ //
    //  Core rule engine
    // ------------------------------------------------------------------ //

    private List<ValidationLog> runAndPersist(Claim claim) {
        // Clear previous results for this claim so re-validation is idempotent
        validationLogRepository.deleteByClaimId(claim.getClaimId());

        List<ValidationLog> logs = new ArrayList<>();

        logs.add(checkBilledAmountPositive(claim));
        logs.add(checkServiceDateNotFuture(claim));
        logs.add(checkProcedureCodeFormat(claim));
        logs.add(checkDiagnosisCodeFormat(claim));
        logs.add(checkPayerIdPresent(claim));
        logs.add(checkProviderIdPresent(claim));

        validationLogRepository.saveAll(logs);
        return logs;
    }

    // ------------------------------------------------------------------ //
    //  Rules
    // ------------------------------------------------------------------ //

    private ValidationLog checkBilledAmountPositive(Claim claim) {
        boolean pass = claim.getBilledAmount() != null
                && claim.getBilledAmount().compareTo(BigDecimal.ZERO) > 0;
        return log(claim.getClaimId(),
                "BILLED_AMOUNT_POSITIVE",
                pass,
                pass ? "Billed amount is positive."
                     : "Billed amount must be greater than zero. Found: " + claim.getBilledAmount());
    }

    private ValidationLog checkServiceDateNotFuture(Claim claim) {
        boolean pass = claim.getServiceDate() != null
                && !claim.getServiceDate().isAfter(LocalDate.now());
        return log(claim.getClaimId(),
                "SERVICE_DATE_NOT_FUTURE",
                pass,
                pass ? "Service date is valid."
                     : "Service date cannot be in the future. Found: " + claim.getServiceDate());
    }

    private ValidationLog checkProcedureCodeFormat(Claim claim) {
        boolean pass = claim.getProcedureCode() != null
                && claim.getProcedureCode().matches("^[A-Z0-9]{4,5}$");
        return log(claim.getClaimId(),
                "PROCEDURE_CODE_FORMAT",
                pass,
                pass ? "Procedure code format is valid."
                     : "Procedure code must be 4-5 alphanumeric characters. Found: "
                       + claim.getProcedureCode());
    }

    private ValidationLog checkDiagnosisCodeFormat(Claim claim) {
        // ICD-10-CM: letter + 2 digits + optional dot + optional alphanumeric suffix
        // Accepts both EDI format (J0690) and clinical format (J06.9)
        boolean pass = claim.getDiagnosisCode() != null
                && claim.getDiagnosisCode().matches("^[A-Z][0-9]{2}\\.?[A-Z0-9]{0,4}$");
        return log(claim.getClaimId(),
                "DIAGNOSIS_CODE_FORMAT",
                pass,
                pass ? "Diagnosis code format is valid."
                     : "Diagnosis code must match ICD-10-CM format. Found: "
                       + claim.getDiagnosisCode());
    }

    private ValidationLog checkPayerIdPresent(Claim claim) {
        boolean pass = claim.getPayerId() != null && !claim.getPayerId().isBlank();
        return log(claim.getClaimId(),
                "PAYER_ID_PRESENT",
                pass,
                pass ? "Payer ID is present."
                     : "Payer ID is missing or blank.");
    }

    private ValidationLog checkProviderIdPresent(Claim claim) {
        boolean pass = claim.getProviderId() != null && !claim.getProviderId().isBlank();
        return log(claim.getClaimId(),
                "PROVIDER_ID_PRESENT",
                pass,
                pass ? "Provider ID is present."
                     : "Provider ID is missing or blank.");
    }

    // ------------------------------------------------------------------ //
    //  Helper
    // ------------------------------------------------------------------ //

    private ValidationLog log(String claimId, String type, boolean pass, String message) {
        ValidationLog vl = new ValidationLog();
        vl.setClaimId(claimId);
        vl.setValidationType(type);
        vl.setStatus(pass ? "PASS" : "FAIL");
        vl.setMessage(message);
        return vl;
    }
}
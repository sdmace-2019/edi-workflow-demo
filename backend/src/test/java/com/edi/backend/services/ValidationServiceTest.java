package com.edi.backend.services;

import com.edi.backend.models.Claim;
import com.edi.backend.models.ValidationLog;
import com.edi.backend.repositories.ClaimRepository;
import com.edi.backend.repositories.ValidationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ValidationService.
 *
 * Repositories are mocked with Mockito — no database or Spring context required.
 * Each test exercises a specific validation rule against a hand-crafted Claim.
 */
@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ValidationLogRepository validationLogRepository;

    @InjectMocks
    private ValidationService validationService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Returns a fully valid claim — all 6 rules should PASS against this. */
    private Claim validClaim() {
        Claim c = new Claim();
        c.setClaimId("CLM-TEST-001");
        c.setPatientId("PAT001");
        c.setProviderId("PROV001");
        c.setPayerId("PAYER001");
        c.setServiceDate(LocalDate.of(2026, 1, 15));   // past date
        c.setProcedureCode("99213");
        c.setDiagnosisCode("Z0000");
        c.setBilledAmount(new BigDecimal("500.00"));
        c.setClaimStatus("RECEIVED");
        c.setEdiType("837");
        return c;
    }

    /** Stubs the repository so validateClaim("CLM-TEST-001") finds our claim. */
    private void stubRepoWith(Claim claim) {
        when(claimRepository.findByClaimId(claim.getClaimId()))
                .thenReturn(Optional.of(claim));
        // saveAll must return the list it receives so the service can return it
        when(validationLogRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    /** Runs validateClaim and returns the log for a specific rule. */
    private ValidationLog logFor(List<ValidationLog> logs, String ruleType) {
        return logs.stream()
                .filter(l -> ruleType.equals(l.getValidationType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No log found for rule: " + ruleType));
    }

    // ── Full-claim tests ──────────────────────────────────────────────────────

    @Test
    void validateClaim_returnsAllSixLogs() {
        Claim claim = validClaim();
        stubRepoWith(claim);

        List<ValidationLog> logs = validationService.validateClaim(claim.getClaimId());

        assertThat(logs).hasSize(6);
        assertThat(logs).extracting(ValidationLog::getStatus).containsOnly("PASS");
    }

    @Test
    void validateClaim_throwsWhenClaimNotFound() {
        when(claimRepository.findByClaimId("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validationService.validateClaim("MISSING"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MISSING");
    }

    @Test
    void validateClaim_isIdempotent() {
        // Re-running validation must delete previous logs before saving new ones
        Claim claim = validClaim();
        stubRepoWith(claim);

        validationService.validateClaim(claim.getClaimId());
        validationService.validateClaim(claim.getClaimId());

        // deleteByClaimId must be called once per validateClaim invocation
        verify(validationLogRepository, times(2)).deleteByClaimId(claim.getClaimId());
        verify(validationLogRepository, times(2)).saveAll(anyList());
    }

    // ── BILLED_AMOUNT_POSITIVE ────────────────────────────────────────────────

    @Test
    void billedAmountPositive_pass() {
        Claim c = validClaim();
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "BILLED_AMOUNT_POSITIVE").getStatus()).isEqualTo("PASS");
    }

    @Test
    void billedAmountPositive_failWhenNull() {
        Claim c = validClaim();
        c.setBilledAmount(null);
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "BILLED_AMOUNT_POSITIVE").getStatus()).isEqualTo("FAIL");
    }

    @Test
    void billedAmountPositive_failWhenZero() {
        Claim c = validClaim();
        c.setBilledAmount(BigDecimal.ZERO);
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "BILLED_AMOUNT_POSITIVE").getStatus()).isEqualTo("FAIL");
    }

    // ── SERVICE_DATE_NOT_FUTURE ───────────────────────────────────────────────

    @Test
    void serviceDateNotFuture_pass() {
        Claim c = validClaim();
        c.setServiceDate(LocalDate.now().minusDays(1));
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "SERVICE_DATE_NOT_FUTURE").getStatus()).isEqualTo("PASS");
    }

    @Test
    void serviceDateNotFuture_failWhenFuture() {
        Claim c = validClaim();
        c.setServiceDate(LocalDate.now().plusDays(1));
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "SERVICE_DATE_NOT_FUTURE").getStatus()).isEqualTo("FAIL");
    }

    @Test
    void serviceDateNotFuture_failWhenNull() {
        Claim c = validClaim();
        c.setServiceDate(null);
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "SERVICE_DATE_NOT_FUTURE").getStatus()).isEqualTo("FAIL");
    }

    // ── PROCEDURE_CODE_FORMAT ─────────────────────────────────────────────────

    @Test
    void procedureCodeFormat_passFiveChars() {
        Claim c = validClaim();
        c.setProcedureCode("99213");  // 5 alphanumeric chars
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "PROCEDURE_CODE_FORMAT").getStatus()).isEqualTo("PASS");
    }

    @Test
    void procedureCodeFormat_passFourChars() {
        Claim c = validClaim();
        c.setProcedureCode("9921");   // 4 alphanumeric chars
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "PROCEDURE_CODE_FORMAT").getStatus()).isEqualTo("PASS");
    }

    @Test
    void procedureCodeFormat_failTooShort() {
        Claim c = validClaim();
        c.setProcedureCode("99");     // only 2 chars
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "PROCEDURE_CODE_FORMAT").getStatus()).isEqualTo("FAIL");
    }

    @Test
    void procedureCodeFormat_failWhenNull() {
        Claim c = validClaim();
        c.setProcedureCode(null);
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "PROCEDURE_CODE_FORMAT").getStatus()).isEqualTo("FAIL");
    }

    // ── DIAGNOSIS_CODE_FORMAT ─────────────────────────────────────────────────

    @Test
    void diagnosisCodeFormat_passEdiFormatNoDot() {
        Claim c = validClaim();
        c.setDiagnosisCode("Z0000");   // EDI format — no dot
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "DIAGNOSIS_CODE_FORMAT").getStatus()).isEqualTo("PASS");
    }

    @Test
    void diagnosisCodeFormat_passClinicalFormatWithDot() {
        Claim c = validClaim();
        c.setDiagnosisCode("Z00.00");  // clinical format — with dot
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "DIAGNOSIS_CODE_FORMAT").getStatus()).isEqualTo("PASS");
    }

    @Test
    void diagnosisCodeFormat_failAllDigits() {
        Claim c = validClaim();
        c.setDiagnosisCode("9999");    // must start with a letter
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "DIAGNOSIS_CODE_FORMAT").getStatus()).isEqualTo("FAIL");
    }

    // ── PAYER_ID_PRESENT ─────────────────────────────────────────────────────

    @Test
    void payerIdPresent_pass() {
        Claim c = validClaim();
        c.setPayerId("PAYER001");
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "PAYER_ID_PRESENT").getStatus()).isEqualTo("PASS");
    }

    @Test
    void payerIdPresent_failWhenBlank() {
        Claim c = validClaim();
        c.setPayerId("   ");   // blank string
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "PAYER_ID_PRESENT").getStatus()).isEqualTo("FAIL");
    }

    @Test
    void payerIdPresent_failWhenNull() {
        Claim c = validClaim();
        c.setPayerId(null);
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "PAYER_ID_PRESENT").getStatus()).isEqualTo("FAIL");
    }

    // ── PROVIDER_ID_PRESENT ───────────────────────────────────────────────────

    @Test
    void providerIdPresent_pass() {
        Claim c = validClaim();
        c.setProviderId("PROV001");
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "PROVIDER_ID_PRESENT").getStatus()).isEqualTo("PASS");
    }

    @Test
    void providerIdPresent_failWhenNull() {
        Claim c = validClaim();
        c.setProviderId(null);
        stubRepoWith(c);

        List<ValidationLog> logs = validationService.validateClaim(c.getClaimId());
        assertThat(logFor(logs, "PROVIDER_ID_PRESENT").getStatus()).isEqualTo("FAIL");
    }
}

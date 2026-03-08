package com.edi.backend.parsers;

import com.edi.backend.models.Claim;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for Edi837Parser — no Spring context loaded.
 * The parser is a plain @Component with no injected dependencies,
 * so it can be instantiated directly with new Edi837Parser().
 */
class Edi837ParserTest {

    private Edi837Parser parser;

    @BeforeEach
    void setUp() {
        parser = new Edi837Parser();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Joins segments with the standard EDI segment terminator. */
    private String seg(String... segments) {
        return String.join("~", segments) + "~";
    }

    /** Minimal valid 837 EDI with all required fields populated. */
    private String minimalValid837() {
        return seg(
            "ISA*00*          *00*          *ZZ*SENDER         *ZZ*RECEIVER       *260101*1200*^*00501*000000001*0*P*:",
            "GS*HC*SENDER*RECEIVER*20260101*1200*1*X*005010X222A1",
            "ST*837*0001",
            "NM1*IL*1*SMITH*JOHN****MI*PAT001",      // subscriber → patientId = PAT001
            "NM1*PR*2*AETNA*****PI*PAYER001",        // payer → payerId = PAYER001
            "CLM*CLM-001*500.00",                    // claimId + billedAmount
            "NM1*82*1*DOE*JANE****XX*PROV001",       // rendering provider → providerId = PROV001
            "DTP*472*D8*20260115",                   // service date
            "SV1*HC:99213*100.00*UN*1",              // procedure code with HC: qualifier
            "HI*ABK:Z0000",                          // diagnosis code with ABK: qualifier
            "SE*10*0001",
            "GE*1*1",
            "IEA*1*000000001"
        );
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void returnsEmptyListForNullInput() {
        assertThat(parser.parseClaims(null, "test")).isEmpty();
    }

    @Test
    void returnsEmptyListForBlankInput() {
        assertThat(parser.parseClaims("   ", "test")).isEmpty();
    }

    @Test
    void parsesStandardClaim() {
        List<Claim> claims = parser.parseClaims(minimalValid837(), "sample_837.edi");

        assertThat(claims).hasSize(1);
        Claim c = claims.get(0);
        assertThat(c.getClaimId()).isEqualTo("CLM-001");
        assertThat(c.getBilledAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(c.getPatientId()).isEqualTo("PAT001");
        assertThat(c.getPayerId()).isEqualTo("PAYER001");
        assertThat(c.getProviderId()).isEqualTo("PROV001");
        assertThat(c.getServiceDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(c.getProcedureCode()).isEqualTo("99213");
        assertThat(c.getDiagnosisCode()).isEqualTo("Z0000");
        assertThat(c.getEdiType()).isEqualTo("837");
        assertThat(c.getClaimStatus()).isEqualTo("RECEIVED");
        assertThat(c.getRawEdiReference()).isEqualTo("sample_837.edi");
    }

    @Test
    void parsesMultipleClaims() {
        String edi = seg(
            "ST*837*0001",
            "NM1*IL*1*SMITH*JOHN****MI*PAT001",
            "NM1*PR*2*AETNA*****PI*PAYER001",
            "CLM*CLM-001*500.00",
            "DTP*472*D8*20260115",
            "SV1*HC:99213*100.00*UN*1",
            "HI*ABK:Z0000",
            "CLM*CLM-002*750.00",
            "DTP*472*D8*20260201",
            "SV1*HC:99214*150.00*UN*1",
            "HI*ABK:Z0001",
            "SE*12*0001"
        );

        List<Claim> claims = parser.parseClaims(edi, "file.edi");

        assertThat(claims).hasSize(2);
        assertThat(claims.get(0).getClaimId()).isEqualTo("CLM-001");
        assertThat(claims.get(1).getClaimId()).isEqualTo("CLM-002");
        assertThat(claims.get(1).getBilledAmount()).isEqualByComparingTo(new BigDecimal("750.00"));
    }

    @Test
    void patientIdFromNm1ILBeforeClm() {
        // NM1*IL appears before CLM — loop-level capture must apply to subsequent CLM
        String edi = seg(
            "ST*837*0001",
            "NM1*IL*1*SMITH*JOHN****MI*PAT-IL-001",
            "CLM*CLM-001*100.00",
            "SE*3*0001"
        );

        List<Claim> claims = parser.parseClaims(edi, "f.edi");
        assertThat(claims.get(0).getPatientId()).isEqualTo("PAT-IL-001");
    }

    @Test
    void patientIdFromNm1QC() {
        // NM1*QC (patient qualifier) should also populate patientId
        String edi = seg(
            "ST*837*0001",
            "CLM*CLM-001*100.00",
            "NM1*QC*1*JONES*BOB****MI*PAT-QC-001",
            "SE*3*0001"
        );

        List<Claim> claims = parser.parseClaims(edi, "f.edi");
        assertThat(claims.get(0).getPatientId()).isEqualTo("PAT-QC-001");
    }

    @Test
    void providerIdFromNm1_82() {
        String edi = seg(
            "ST*837*0001",
            "CLM*CLM-001*100.00",
            "NM1*82*1*DOE*JANE****XX*PROV-082",
            "SE*3*0001"
        );

        List<Claim> claims = parser.parseClaims(edi, "f.edi");
        assertThat(claims.get(0).getProviderId()).isEqualTo("PROV-082");
    }

    @Test
    void payerIdFromNm1PR() {
        String edi = seg(
            "ST*837*0001",
            "NM1*PR*2*BCBS*****PI*PAYER-PR-001",
            "CLM*CLM-001*100.00",
            "SE*3*0001"
        );

        List<Claim> claims = parser.parseClaims(edi, "f.edi");
        assertThat(claims.get(0).getPayerId()).isEqualTo("PAYER-PR-001");
    }

    @Test
    void parsesServiceDateFromDtp472() {
        String edi = seg(
            "ST*837*0001",
            "CLM*CLM-001*100.00",
            "DTP*472*D8*20260315",
            "SE*3*0001"
        );

        List<Claim> claims = parser.parseClaims(edi, "f.edi");
        assertThat(claims.get(0).getServiceDate()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void procedureCodeStripsHcQualifier() {
        // SV1*HC:99213 — the "HC:" prefix must be stripped, leaving just "99213"
        String edi = seg(
            "ST*837*0001",
            "CLM*CLM-001*100.00",
            "SV1*HC:99213*100.00*UN*1",
            "SE*3*0001"
        );

        List<Claim> claims = parser.parseClaims(edi, "f.edi");
        assertThat(claims.get(0).getProcedureCode()).isEqualTo("99213");
    }

    @Test
    void procedureCodeWithNoQualifier() {
        // SV1*99213 — no colon separator, code used as-is
        String edi = seg(
            "ST*837*0001",
            "CLM*CLM-001*100.00",
            "SV1*99213*100.00*UN*1",
            "SE*3*0001"
        );

        List<Claim> claims = parser.parseClaims(edi, "f.edi");
        assertThat(claims.get(0).getProcedureCode()).isEqualTo("99213");
    }

    @Test
    void diagnosisCodeStripsQualifier() {
        // HI*ABK:Z0000 — qualifier "ABK:" must be stripped
        String edi = seg(
            "ST*837*0001",
            "CLM*CLM-001*100.00",
            "HI*ABK:Z0000",
            "SE*3*0001"
        );

        List<Claim> claims = parser.parseClaims(edi, "f.edi");
        assertThat(claims.get(0).getDiagnosisCode()).isEqualTo("Z0000");
    }

    @Test
    void setsDefaultEdiTypeAndClaimStatus() {
        List<Claim> claims = parser.parseClaims(minimalValid837(), "file.edi");

        assertThat(claims.get(0).getEdiType()).isEqualTo("837");
        assertThat(claims.get(0).getClaimStatus()).isEqualTo("RECEIVED");
    }

    @Test
    void setsRawEdiReference() {
        List<Claim> claims = parser.parseClaims(minimalValid837(), "my_source_file.edi");

        assertThat(claims.get(0).getRawEdiReference()).isEqualTo("my_source_file.edi");
    }

    @Test
    void claimWithBlankBilledAmountDoesNotCrash() {
        // CLM segment with empty amount field — parser must not throw
        String edi = seg(
            "ST*837*0001",
            "CLM*CLM-001*",
            "SE*2*0001"
        );

        List<Claim> claims = parser.parseClaims(edi, "f.edi");

        // Claim is still added (validation rules handle missing fields, not the parser)
        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).getBilledAmount()).isNull();
    }

    @Test
    void nm1BeforeAnyClmDoesNotCrash() {
        // NM1 for entity 82 (provider) appears before any CLM — must not throw NPE
        String edi = seg(
            "ST*837*0001",
            "NM1*82*1*ORPHAN*PROVIDER****XX*PROV-ORPHAN",
            "SE*2*0001"
        );

        // No CLM in file → empty list, no exception
        assertThat(parser.parseClaims(edi, "f.edi")).isEmpty();
    }
}

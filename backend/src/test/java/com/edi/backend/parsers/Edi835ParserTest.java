package com.edi.backend.parsers;

import com.edi.backend.models.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for Edi835Parser — no Spring context loaded.
 * The parser is a stateless @Component with no injected dependencies.
 */
class Edi835ParserTest {

    private Edi835Parser parser;

    @BeforeEach
    void setUp() {
        parser = new Edi835Parser();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String seg(String... segments) {
        return String.join("~", segments) + "~";
    }

    /** Minimal valid 835 with one CLP, all header fields populated. */
    private String minimal835() {
        return seg(
            "ISA*00*          *00*          *ZZ*PAYER001       *ZZ*PROVIDER001    *260101*1200*^*00501*000000001*0*P*:",
            "GS*HP*PAYER001*PROVIDER001*20260101*1200*1*X*005010X221A1",
            "ST*835*0001",
            "BPR*I*400.00*C*ACH",         // BPR04 = ACH
            "TRN*1*ERA-2026-00001",        // ERA reference ID
            "DTM*405*20260101",            // payment date
            "N1*PR*AETNA*PI*PAYER001",     // payer
            "N1*PE*DR SMITH*XX*PROV001",   // provider/payee
            "CLP*CLM-001*1*500.00*400.00", // claim billed=500, paid=400
            "SE*9*0001",
            "GE*1*1",
            "IEA*1*000000001"
        );
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void returnsEmptyForNullInput() {
        assertThat(parser.parsePayments(null, "test.edi")).isEmpty();
    }

    @Test
    void returnsEmptyForBlankInput() {
        assertThat(parser.parsePayments("   ", "test.edi")).isEmpty();
    }

    @Test
    void parsesStandardPayment() {
        List<Payment> payments = parser.parsePayments(minimal835(), "sample_835.edi");

        assertThat(payments).hasSize(1);
        Payment p = payments.get(0);
        assertThat(p.getClaimId()).isEqualTo("CLM-001");
        assertThat(p.getPaymentAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(p.getPayerId()).isEqualTo("PAYER001");
        assertThat(p.getProviderId()).isEqualTo("PROV001");
        assertThat(p.getPaymentDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(p.getPaymentMethod()).isEqualTo("ACH");
        assertThat(p.getAdjAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(p.getPlbAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void paymentIdFormatIsEraIdPlusClaimId() {
        List<Payment> payments = parser.parsePayments(minimal835(), "sample_835.edi");

        // paymentId must be era-reference + "-" + claimId so it is globally unique
        assertThat(payments.get(0).getPaymentId()).isEqualTo("ERA-2026-00001-CLM-001");
    }

    @Test
    void parsesMultipleClpSegments() {
        String edi = seg(
            "ST*835*0001",
            "TRN*1*ERA-2026-00002",
            "DTM*405*20260115",
            "N1*PR*BCBS*PI*PAYER002",
            "N1*PE*DR JONES*XX*PROV002",
            "CLP*CLM-001*1*500.00*400.00",
            "CLP*CLM-002*1*750.00*550.00",
            "SE*7*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");

        assertThat(payments).hasSize(2);
        assertThat(payments.get(0).getClaimId()).isEqualTo("CLM-001");
        assertThat(payments.get(0).getPaymentAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(payments.get(1).getClaimId()).isEqualTo("CLM-002");
        assertThat(payments.get(1).getPaymentAmount()).isEqualByComparingTo(new BigDecimal("550.00"));
    }

    @Test
    void parsesPaymentDateFromDtm405() {
        String edi = seg(
            "ST*835*0001",
            "DTM*405*20260315",
            "CLP*CLM-001*1*100.00*80.00",
            "SE*3*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");
        assertThat(payments.get(0).getPaymentDate()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void payerIdFromN1PR() {
        String edi = seg(
            "ST*835*0001",
            "N1*PR*CIGNA*PI*PAYER-CIGNA",
            "CLP*CLM-001*1*100.00*80.00",
            "SE*3*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");
        assertThat(payments.get(0).getPayerId()).isEqualTo("PAYER-CIGNA");
    }

    @Test
    void providerIdFromN1PE() {
        String edi = seg(
            "ST*835*0001",
            "N1*PE*DR DOE*XX*PROV-PE-001",
            "CLP*CLM-001*1*100.00*80.00",
            "SE*3*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");
        assertThat(payments.get(0).getProviderId()).isEqualTo("PROV-PE-001");
    }

    @Test
    void providerIdFallsBackToNm1_82() {
        // No N1*PE header — NM1*82 inside the CLP block should supply providerId
        String edi = seg(
            "ST*835*0001",
            "CLP*CLM-001*1*100.00*80.00",
            "NM1*82*1*DOE*JANE****XX*PROV-NM1-082",
            "SE*3*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");
        assertThat(payments.get(0).getProviderId()).isEqualTo("PROV-NM1-082");
    }

    @Test
    void casAdjAmountAccumulated() {
        // Two CAS amounts — adj_amount should be the absolute sum
        // CAS element layout: CAS*groupCode*reason1*amount1[*reason2*amount2...]
        // Amounts are at indices 3, 6, 9, ... (every 3rd starting from 3)
        String edi = seg(
            "ST*835*0001",
            "CLP*CLM-001*1*500.00*400.00",
            "CAS*CO*45*50.00",
            "CAS*PR*1*50.00",
            "SE*4*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");
        assertThat(payments.get(0).getAdjAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void plbAllocatedToReferencedClaim() {
        // PLB references CLM-001 by "CV:CLM-001" — plbAmount on that payment must be 50.00
        String edi = seg(
            "ST*835*0001",
            "TRN*1*ERA-PLB-001",
            "N1*PE*DR TEST*XX*PROV001",
            "CLP*CLM-001*1*500.00*400.00",
            "PLB*PROV001*20260101*CV:CLM-001*-50.00",
            "SE*5*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");
        assertThat(payments.get(0).getPlbAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void unallocatedPlbAppliedToFirstPayment() {
        // PLB with no colon claim ref — applied to first payment
        String edi = seg(
            "ST*835*0001",
            "TRN*1*ERA-PLB-002",
            "CLP*CLM-001*1*500.00*400.00",
            "CLP*CLM-002*1*750.00*600.00",
            "PLB*PROV001*20260101*WO*-25.00",   // no colon → unallocated
            "SE*5*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");
        // Unallocated PLB goes to the first payment only
        assertThat(payments.get(0).getPlbAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(payments.get(1).getPlbAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void paymentAmountFromClpElement4() {
        // CLP*claimId*claimStatus*billedAmt*paidAmt — paidAmt is element index 4
        String edi = seg(
            "ST*835*0001",
            "CLP*CLM-001*1*999.00*123.45",
            "SE*2*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");
        assertThat(payments.get(0).getPaymentAmount()).isEqualByComparingTo(new BigDecimal("123.45"));
    }

    @Test
    void paymentDateDefaultsToTodayWhenMissing() {
        // No DTM*405 — parser must set a non-null date (LocalDate.now() fallback)
        String edi = seg(
            "ST*835*0001",
            "CLP*CLM-001*1*100.00*80.00",
            "SE*2*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");
        assertThat(payments.get(0).getPaymentDate()).isNotNull();
    }

    @Test
    void paymentMethodFromBprElement4() {
        String edi = seg(
            "ST*835*0001",
            "BPR*I*400.00*C*CHK",   // BPR04 = CHK (check)
            "CLP*CLM-001*1*100.00*80.00",
            "SE*3*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");
        assertThat(payments.get(0).getPaymentMethod()).isEqualTo("CHK");
    }

    @Test
    void multipleClpsShareHeaderPayerAndProvider() {
        // N1*PR and N1*PE are header-level — must propagate to all CLPs
        String edi = seg(
            "ST*835*0001",
            "N1*PR*HUMANA*PI*PAYER-H",
            "N1*PE*DR SHARED*XX*PROV-S",
            "CLP*CLM-001*1*100.00*80.00",
            "CLP*CLM-002*1*200.00*160.00",
            "SE*5*0001"
        );

        List<Payment> payments = parser.parsePayments(edi, "f.edi");
        assertThat(payments).allSatisfy(p -> {
            assertThat(p.getPayerId()).isEqualTo("PAYER-H");
            assertThat(p.getProviderId()).isEqualTo("PROV-S");
        });
    }
}

package com.edi.backend;

import com.edi.backend.models.Claim;
import com.edi.backend.models.Payment;
import com.edi.backend.repositories.ClaimRepository;
import com.edi.backend.repositories.PaymentRepository;
import com.edi.backend.repositories.ValidationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration test — loads the complete Spring ApplicationContext
 * and connects to the real PostgreSQL database configured in application.yaml.
 *
 * Prerequisites before running:
 *   1. PostgreSQL must be running on localhost:5432
 *   2. Database 'edi_demo' must exist with user 'edi_user'
 *   3. All four SQL migrations must have been applied
 *      (01_create_claims.sql through 04_create_views.sql)
 *   4. The 'edi-samples/' directory must be present and contain
 *      sample_837.edi and sample_835.edi
 *
 * These tests are intentionally stateful — each @BeforeEach clears
 * the claims, payments, and validation_logs tables so results are
 * deterministic regardless of what data already exists in dev.
 *
 * Run individually:
 *   mvn test -Dtest=IngestIntegrationTest
 *
 * Run with all tests (requires DB):
 *   mvn test
 */
@SpringBootTest
@AutoConfigureMockMvc
class IngestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ValidationLogRepository validationLogRepository;

    @BeforeEach
    void clearDatabase() {
        // Delete in dependency order: validation_logs → payments → claims
        validationLogRepository.deleteAll();
        paymentRepository.deleteAll();
        claimRepository.deleteAll();
    }

    // ── POST /api/intake/samples/ingest ───────────────────────────────────────

    @Test
    void ingestSamples_returns200AndSavesClaimsAndPayments() throws Exception {
        mockMvc.perform(post("/api/intake/samples/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filesProcessed").value(2))
                .andExpect(jsonPath("$.totalClaimsSaved").value(2))
                .andExpect(jsonPath("$.totalPaymentsSaved").value(2));

        // Verify rows actually landed in the DB
        List<Claim> claims = claimRepository.findAll();
        assertThat(claims).hasSize(2);
        assertThat(claims).extracting(Claim::getClaimStatus).containsOnly("RECEIVED");

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(2);
    }

    @Test
    void ingestSamples_deduplicatesOnReIngest() throws Exception {
        // First ingest
        mockMvc.perform(post("/api/intake/samples/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClaimsSaved").value(2));

        // Second ingest — existing claims must be skipped, not duplicated
        mockMvc.perform(post("/api/intake/samples/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClaimsSaved").value(0));

        // DB must still have exactly 2 claims
        assertThat(claimRepository.count()).isEqualTo(2);
    }

    // ── GET /api/claims ───────────────────────────────────────────────────────

    @Test
    void getClaims_returns200WithPaginatedResults() throws Exception {
        // Seed data via ingest
        mockMvc.perform(post("/api/intake/samples/ingest")).andExpect(status().isOk());

        mockMvc.perform(get("/api/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].claimStatus").value("RECEIVED"));
    }

    @Test
    void getClaims_returnsEmptyPageWhenNoData() throws Exception {
        mockMvc.perform(get("/api/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ── GET /api/claims/{claimId} ─────────────────────────────────────────────

    @Test
    void getClaim_returns200ForExistingClaim() throws Exception {
        mockMvc.perform(post("/api/intake/samples/ingest")).andExpect(status().isOk());

        // Grab the first claim id from DB to use in the lookup
        String claimId = claimRepository.findAll().get(0).getClaimId();

        mockMvc.perform(get("/api/claims/" + claimId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(claimId));
    }

    @Test
    void getClaim_returns404ForUnknownClaim() throws Exception {
        mockMvc.perform(get("/api/claims/DOES-NOT-EXIST"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/payments ─────────────────────────────────────────────────────

    @Test
    void getPayments_returns200WithTwoPaymentsAfterIngest() throws Exception {
        mockMvc.perform(post("/api/intake/samples/ingest")).andExpect(status().isOk());

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ── GET /api/dashboard ────────────────────────────────────────────────────

    @Test
    void getDashboard_returns200WithStructuredSummary() throws Exception {
        mockMvc.perform(post("/api/intake/samples/ingest")).andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claims.total").value(2))
                .andExpect(jsonPath("$.payments.total").value(2))
                .andExpect(jsonPath("$.validation").exists());
    }
}

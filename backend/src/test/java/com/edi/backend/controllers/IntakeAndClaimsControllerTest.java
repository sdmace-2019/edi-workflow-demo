package com.edi.backend.controllers;

import com.edi.backend.models.Claim;
import com.edi.backend.repositories.ClaimRepository;
import com.edi.backend.repositories.ValidationLogRepository;
import com.edi.backend.services.SampleEdiIngestService;
import com.edi.backend.services.SampleFolderIntakeService;
import com.edi.backend.services.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test — loads only the web layer (controllers, filters, error handlers).
 * All services and repositories are replaced with Mockito mocks.
 * No database connection is required; runs in well under 1 second.
 *
 * Why @WebMvcTest?
 *   - Verifies HTTP status codes, Content-Type, and JSON structure
 *   - Tests controller-level error handling (404, 500)
 *   - Completely independent of the database or file system
 */
@WebMvcTest(controllers = {IntakeController.class, ClaimsController.class})
class IntakeAndClaimsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Mocked dependencies ───────────────────────────────────────────────────
    // @MockitoBean replaces each bean in the Spring test context with a Mockito mock.

    @MockitoBean
    private SampleFolderIntakeService sampleFolderIntakeService;

    @MockitoBean
    private SampleEdiIngestService sampleEdiIngestService;

    @MockitoBean
    private ClaimRepository claimRepository;

    @MockitoBean
    private ValidationLogRepository validationLogRepository;

    @MockitoBean
    private ValidationService validationService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Claim sampleClaim() {
        Claim c = new Claim();
        c.setClaimId("CLM-MOCK-001");
        c.setPatientId("PAT001");
        c.setProviderId("PROV001");
        c.setPayerId("PAYER001");
        c.setServiceDate(LocalDate.of(2026, 1, 15));
        c.setProcedureCode("99213");
        c.setDiagnosisCode("Z0000");
        c.setBilledAmount(new BigDecimal("500.00"));
        c.setClaimStatus("RECEIVED");
        c.setEdiType("837");
        return c;
    }

    // ── POST /api/intake/samples/ingest ───────────────────────────────────────

    @Test
    void ingestSamples_returns200WithIngestResult() throws Exception {
        SampleEdiIngestService.IngestResult result = new SampleEdiIngestService.IngestResult(
                "/edi-samples", 2, 2, 2,
                List.of(
                        new SampleEdiIngestService.FileResult("sample_837.edi", "837", 2, 0, 0, 0, "OK"),
                        new SampleEdiIngestService.FileResult("sample_835.edi", "835", 0, 0, 2, 0, "OK")
                )
        );
        when(sampleEdiIngestService.ingestSamples()).thenReturn(result);

        mockMvc.perform(post("/api/intake/samples/ingest"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.filesProcessed").value(2))
                .andExpect(jsonPath("$.totalClaimsSaved").value(2))
                .andExpect(jsonPath("$.totalPaymentsSaved").value(2))
                .andExpect(jsonPath("$.results[0].fileName").value("sample_837.edi"))
                .andExpect(jsonPath("$.results[1].fileName").value("sample_835.edi"));
    }

    @Test
    void ingestSamples_returns500WhenServiceThrows() throws Exception {
        when(sampleEdiIngestService.ingestSamples())
                .thenThrow(new RuntimeException("Disk read failure"));

        mockMvc.perform(post("/api/intake/samples/ingest"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void ingestSamples_returns400WhenDirNotFound() throws Exception {
        when(sampleEdiIngestService.ingestSamples())
                .thenThrow(new IllegalArgumentException("Sample dir not found"));

        mockMvc.perform(post("/api/intake/samples/ingest"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/claims ───────────────────────────────────────────────────────

    @Test
    void getClaims_returns200WithPage() throws Exception {
        // ClaimsController calls claimRepository.findAll(Pageable) directly,
        // so we stub the repository (not a service).
        org.springframework.data.domain.Page<Claim> page =
                new org.springframework.data.domain.PageImpl<>(List.of(sampleClaim()));
        when(claimRepository.findAll(org.mockito.ArgumentMatchers.any(
                org.springframework.data.domain.Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/claims"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.content[0].claimId").value("CLM-MOCK-001"))
                .andExpect(jsonPath("$.content[0].claimStatus").value("RECEIVED"));
    }

    // ── GET /api/claims/{claimId} ─────────────────────────────────────────────

    @Test
    void getClaim_returns200WhenFound() throws Exception {
        when(claimRepository.findByClaimId("CLM-MOCK-001"))
                .thenReturn(java.util.Optional.of(sampleClaim()));

        mockMvc.perform(get("/api/claims/CLM-MOCK-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value("CLM-MOCK-001"))
                .andExpect(jsonPath("$.billedAmount").value(500.00));
    }

    @Test
    void getClaim_returns404WhenNotFound() throws Exception {
        when(claimRepository.findByClaimId("MISSING"))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/claims/MISSING"))
                .andExpect(status().isNotFound());
    }
}

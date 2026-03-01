package com.edi.backend.controllers;

import com.edi.backend.models.Claim;
import com.edi.backend.models.ValidationLog;
import com.edi.backend.repositories.ClaimRepository;
import com.edi.backend.repositories.ValidationLogRepository;
import com.edi.backend.services.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
public class ClaimsController {

    private final ClaimRepository claimRepository;
    private final ValidationLogRepository validationLogRepository;
    private final ValidationService validationService;

    public ClaimsController(ClaimRepository claimRepository,
                            ValidationLogRepository validationLogRepository,
                            ValidationService validationService) {
        this.claimRepository = claimRepository;
        this.validationLogRepository = validationLogRepository;
        this.validationService = validationService;
    }

    /** GET /api/claims — list all claims */
    @GetMapping
    public List<Claim> getAllClaims() {
        return claimRepository.findAll();
    }

    /** GET /api/claims/{claimId} — single claim */
    @GetMapping("/{claimId}")
    public ResponseEntity<Claim> getClaim(@PathVariable String claimId) {
        return claimRepository.findByClaimId(claimId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/claims/{claimId}/validations — validation results for a claim */
    @GetMapping("/{claimId}/validations")
    public ResponseEntity<List<ValidationLog>> getValidations(@PathVariable String claimId) {
        if (!claimRepository.existsByClaimId(claimId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(
                validationLogRepository.findByClaimIdOrderByValidationTypeAsc(claimId));
    }

    /** POST /api/claims/{claimId}/validate — run validation for one claim */
    @PostMapping("/{claimId}/validate")
    public ResponseEntity<List<ValidationLog>> validateClaim(@PathVariable String claimId) {
        try {
            return ResponseEntity.ok(validationService.validateClaim(claimId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** POST /api/claims/validate-all — run validation for every claim */
    @PostMapping("/validate-all")
    public ResponseEntity<Map<String, Integer>> validateAll() {
        int total = validationService.validateAll();
        return ResponseEntity.ok(Map.of("logsWritten", total));
    }
}
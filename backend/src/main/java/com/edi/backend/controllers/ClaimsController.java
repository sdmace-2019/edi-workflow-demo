package com.edi.backend.controllers;

import com.edi.backend.models.Claim;
import com.edi.backend.repositories.ClaimRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimsController {

    private final ClaimRepository claimRepository;

    public ClaimsController(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    @GetMapping
    public List<Claim> list() {
        return claimRepository.findAll();
    }

    @GetMapping("/{claimId}")
    public Claim getByClaimId(@PathVariable String claimId) {
        return claimRepository.findByClaimId(claimId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Claim not found: " + claimId));
    }
}
package com.edi.backend.repositories;

import com.edi.backend.models.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimId(String claimId);
    boolean existsByClaimId(String claimId);
}

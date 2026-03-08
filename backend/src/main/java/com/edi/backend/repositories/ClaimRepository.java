package com.edi.backend.repositories;

import com.edi.backend.models.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    boolean existsByClaimId(String claimId);
    Optional<Claim> findByClaimId(String claimId);

    @Query("SELECT c.claimStatus, COUNT(c) FROM Claim c GROUP BY c.claimStatus")
    List<Object[]> countByStatus();

    @Query("SELECT COALESCE(SUM(c.billedAmount), 0) FROM Claim c")
    BigDecimal sumBilledAmount();
}

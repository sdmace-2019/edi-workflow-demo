package com.edi.backend.repositories;

import com.edi.backend.models.ValidationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ValidationLogRepository extends JpaRepository<ValidationLog, Long> {
    List<ValidationLog> findByClaimIdOrderByValidationTypeAsc(String claimId);
    boolean existsByClaimIdAndValidationType(String claimId, String validationType);
    void deleteByClaimId(String claimId);
}
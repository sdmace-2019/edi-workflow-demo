package com.edi.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", unique = true, nullable = false)
    private String claimId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "payer_id", nullable = false)
    private String payerId;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "procedure_code", nullable = false)
    private String procedureCode;

    @Column(name = "diagnosis_code", nullable = false)
    private String diagnosisCode;

    @Column(name = "billed_amount", nullable = false)
    private BigDecimal billedAmount;

    @Column(name = "claim_status")
    private String claimStatus;

    @Column(name = "edi_type")
    private String ediType;

    @Column(name = "raw_edi_reference")
    private String rawEdiReference;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Apply defaults here so JPA always persists them correctly
        if (claimStatus == null) claimStatus = "RECEIVED";
        if (ediType == null) ediType = "837";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

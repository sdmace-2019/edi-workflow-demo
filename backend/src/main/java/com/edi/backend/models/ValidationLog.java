package com.edi.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "validation_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", length = 50)
    private String claimId;

    @Column(name = "validation_type", nullable = false, length = 50)
    private String validationType;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
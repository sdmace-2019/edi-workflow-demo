package com.edi.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, length = 50)
    private String paymentId;

    @Column(name = "claim_id", length = 50)
    private String claimId;

    @Column(name = "provider_id", nullable = false, length = 50)
    private String providerId;

    @Column(name = "payer_id", nullable = false, length = 50)
    private String payerId;

    @Column(name = "payment_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "adj_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal adjAmount;

    @Column(name = "plb_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal plbAmount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Column(name = "era_status", length = 20)
    private String eraStatus;

    @Column(name = "ctime")
    private LocalDateTime ctime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (ctime == null) ctime = now;
        if (updateTime == null) updateTime = now;
        if (eraStatus == null) eraStatus = "RECEIVED";
        if (adjAmount == null) adjAmount = BigDecimal.ZERO;
        if (plbAmount == null) plbAmount = BigDecimal.ZERO;
    }
}

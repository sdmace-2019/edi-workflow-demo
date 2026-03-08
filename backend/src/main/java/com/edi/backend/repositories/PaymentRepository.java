package com.edi.backend.repositories;

import com.edi.backend.models.Payment;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByPaymentId(String paymentId);
    Optional<Payment> findByPaymentId(String paymentId);

    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM Payment p")
    BigDecimal sumPaymentAmount();

    @Query("SELECT COALESCE(SUM(p.adjAmount), 0) FROM Payment p")
    BigDecimal sumAdjAmount();
}

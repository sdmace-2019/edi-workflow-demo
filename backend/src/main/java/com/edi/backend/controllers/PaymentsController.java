package com.edi.backend.controllers;

import com.edi.backend.models.Payment;
import com.edi.backend.repositories.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentsController {

    private final PaymentRepository paymentRepository;

    public PaymentsController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /** GET /api/payments — list all payments */
    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /** GET /api/payments/summary — total paid, total adj, count */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPayments", paymentRepository.count());
        summary.put("totalPaid", paymentRepository.sumPaymentAmount());
        summary.put("totalAdj", paymentRepository.sumAdjAmount());
        return ResponseEntity.ok(summary);
    }

    /** GET /api/payments/{paymentId} — single payment by payment_id */
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(@PathVariable String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
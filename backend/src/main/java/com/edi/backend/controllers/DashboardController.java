package com.edi.backend.controllers;

import com.edi.backend.repositories.ClaimRepository;
import com.edi.backend.repositories.PaymentRepository;
import com.edi.backend.repositories.ValidationLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ClaimRepository claimRepository;
    private final PaymentRepository paymentRepository;
    private final ValidationLogRepository validationLogRepository;

    public DashboardController(ClaimRepository claimRepository,
                               PaymentRepository paymentRepository,
                               ValidationLogRepository validationLogRepository) {
        this.claimRepository = claimRepository;
        this.paymentRepository = paymentRepository;
        this.validationLogRepository = validationLogRepository;
    }

    /** GET /api/dashboard — combined claims + payments + validation summary */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // Claims section
        Map<String, Object> claims = new HashMap<>();
        claims.put("total", claimRepository.count());
        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : claimRepository.countByStatus()) {
            byStatus.put((String) row[0], (Long) row[1]);
        }
        claims.put("byStatus", byStatus);
        claims.put("totalBilled", claimRepository.sumBilledAmount());
        dashboard.put("claims", claims);

        // Payments section
        Map<String, Object> payments = new HashMap<>();
        payments.put("total", paymentRepository.count());
        payments.put("totalPaid", paymentRepository.sumPaymentAmount());
        payments.put("totalAdj", paymentRepository.sumAdjAmount());
        dashboard.put("payments", payments);

        // Validation section
        Map<String, Object> validation = new HashMap<>();
        validation.put("totalLogs", validationLogRepository.count());
        validation.put("passCount", validationLogRepository.countByStatus("PASS"));
        validation.put("failCount", validationLogRepository.countByStatus("FAIL"));
        dashboard.put("validation", validation);

        return ResponseEntity.ok(dashboard);
    }
}

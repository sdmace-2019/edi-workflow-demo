package com.edi.backend.parsers;

import com.edi.backend.models.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stateless parser for X12 835 ERA (Electronic Remittance Advice) files.
 *
 * Produces one {@link Payment} record per CLP (claim-level) segment.
 * PLB (provider-level adjustment) amounts are distributed to the
 * claim referenced in the PLB adjustment reason code (e.g. CV:CLM-xxx).
 * If a PLB references no specific claim, it is applied to the first
 * payment in the batch.
 *
 * Reads:
 *   BPR — payment method (BPR04)
 *   TRN — ERA reference ID (TRN02)
 *   DTM*405 — payment date
 *   N1*PR — payer ID (N1*04)
 *   N1*PE — provider/payee ID (N1*04)
 *   CLP — per-claim: claim ID (CLP01), paid amount (CLP04)
 *   NM1*82 — rendering provider NPI fallback
 *   CAS — adjustment amounts accumulated into adj_amount
 *   PLB — provider-level adjustments allocated to claim
 */
@Component
public class Edi835Parser {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<Payment> parsePayments(String ediText, String sourceRef) {
        if (ediText == null || ediText.isBlank()) return List.of();

        String[] segments = ediText.split("~\\s*");

        // Header-level values shared across all CLPs
        String eraId        = null;
        String paymentMethod = "UNKNOWN";
        LocalDate paymentDate = null;
        String payerId      = null;
        String headerProviderId = null;

        // Per-CLP state
        List<Payment> payments = new ArrayList<>();
        Payment current = null;

        // Track PLB adjustments: claimId -> total PLB amount
        Map<String, BigDecimal> plbByClaimId = new HashMap<>();
        // PLB with no specific claim -> accumulate to apply to first payment
        BigDecimal unallocatedPlb = BigDecimal.ZERO;

        for (String raw : segments) {
            String seg = raw.trim();
            if (seg.isEmpty()) continue;

            String[] el = seg.split("\\*", -1);
            String tag = el[0];

            switch (tag) {

                // BPR: BPR01=txType, BPR04=paymentMethod
                case "BPR" -> {
                    if (el.length > 4 && !el[4].isBlank()) {
                        paymentMethod = el[4]; // e.g. ACH, CHK
                    }
                }

                // TRN: TRN02 = ERA reference ID
                case "TRN" -> {
                    if (el.length > 2 && !el[2].isBlank()) {
                        eraId = el[2];
                    }
                }

                // DTM*405 = payment date; DTM qualifier is el[1]
                case "DTM" -> {
                    String qualifier = el.length > 1 ? el[1] : "";
                    String dateVal   = el.length > 2 ? el[2] : "";
                    if ("405".equals(qualifier) && !dateVal.isBlank()) {
                        paymentDate = LocalDate.parse(dateVal, YYYYMMDD);
                    }
                }

                // N1*PR = payer; N1*PE = provider/payee
                case "N1" -> {
                    String entity = el.length > 1 ? el[1] : "";
                    String entityId = el.length > 4 ? el[4] : null;
                    if ("PR".equals(entity) && entityId != null && !entityId.isBlank()) {
                        payerId = entityId;
                    } else if ("PE".equals(entity) && entityId != null && !entityId.isBlank()) {
                        headerProviderId = entityId;
                    }
                }

                // CLP: start of a new claim payment block
                // CLP01=claimId, CLP03=billedAmt, CLP04=paidAmt
                case "CLP" -> {
                    // Finalize previous payment
                    if (current != null) {
                        payments.add(current);
                    }
                    String claimId = el.length > 1 ? el[1].trim() : null;
                    BigDecimal paidAmt = parseBigDecimal(el.length > 4 ? el[4] : null);

                    current = new Payment();
                    current.setClaimId(claimId);
                    current.setPaymentAmount(paidAmt != null ? paidAmt : BigDecimal.ZERO);
                    current.setAdjAmount(BigDecimal.ZERO);
                    current.setPlbAmount(BigDecimal.ZERO);
                    current.setProviderId(headerProviderId != null ? headerProviderId : "UNKNOWN");
                    current.setPayerId(payerId != null ? payerId : "UNKNOWN");
                    current.setPaymentMethod(paymentMethod);
                    current.setPaymentDate(paymentDate);
                    // paymentId = eraId + "-" + claimId — finalized at end when eraId is confirmed
                }

                // NM1*82 — rendering provider; use as providerId if not already set from N1*PE
                case "NM1" -> {
                    String entity = el.length > 1 ? el[1] : "";
                    if ("82".equals(entity) && current != null) {
                        String npi = el.length > 9 ? el[9].trim() : null;
                        if (npi != null && !npi.isBlank() && "UNKNOWN".equals(current.getProviderId())) {
                            current.setProviderId(npi);
                        }
                    }
                }

                // CAS — adjustment amounts; accumulate into current CLP's adjAmount
                case "CAS" -> {
                    if (current != null) {
                        // CAS elements repeat in groups of 3: qualifier, reason, amount
                        // el[0]=CAS, el[1]=groupCode, then triples: el[2]=reason, el[3]=amt, ...
                        for (int i = 3; i < el.length; i += 3) {
                            BigDecimal adjAmt = parseBigDecimal(el[i]);
                            if (adjAmt != null) {
                                current.setAdjAmount(current.getAdjAmount().add(adjAmt.abs()));
                            }
                        }
                    }
                }

                // PLB — provider-level adjustments
                // PLB01=provId, PLB02=fiscalDate, PLB03=reasonCode:claimId, PLB04=amount, ...
                case "PLB" -> {
                    // Elements repeat in pairs: PLB03-reason, PLB04-amount, PLB05-reason2, PLB06-amount2 ...
                    for (int i = 3; i + 1 < el.length; i += 2) {
                        String reasonRef = el[i];       // e.g. "CV:CLM-2026-00001"
                        BigDecimal plbAmt = parseBigDecimal(el[i + 1]);
                        if (plbAmt == null) continue;
                        // Extract claim ID after colon if present
                        if (reasonRef.contains(":")) {
                            String referencedClaimId = reasonRef.substring(reasonRef.indexOf(':') + 1);
                            plbByClaimId.merge(referencedClaimId, plbAmt.abs(), BigDecimal::add);
                        } else {
                            unallocatedPlb = unallocatedPlb.add(plbAmt.abs());
                        }
                    }
                }
            }
        }

        // Finalize last CLP
        if (current != null) {
            payments.add(current);
        }

        // Apply PLB amounts and set paymentId
        boolean firstPayment = true;
        for (Payment p : payments) {
            // Assign payment_id
            String pid = (eraId != null ? eraId : "ERA-UNKNOWN")
                    + "-" + (p.getClaimId() != null ? p.getClaimId() : "UNKNOWN");
            p.setPaymentId(pid);

            // Set payment date fallback
            if (p.getPaymentDate() == null) {
                p.setPaymentDate(LocalDate.now());
            }

            // Apply claim-specific PLB
            if (p.getClaimId() != null) {
                BigDecimal claimPlb = plbByClaimId.getOrDefault(p.getClaimId(), BigDecimal.ZERO);
                p.setPlbAmount(claimPlb);
            }

            // Apply unallocated PLB to first payment
            if (firstPayment && unallocatedPlb.compareTo(BigDecimal.ZERO) > 0) {
                p.setPlbAmount(p.getPlbAmount().add(unallocatedPlb));
                firstPayment = false;
            }
        }

        return payments;
    }

    private static BigDecimal parseBigDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

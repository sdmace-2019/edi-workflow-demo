package com.edi.backend.parsers;

import com.edi.backend.models.Claim;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class Edi837Parser {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<Claim> parseClaims(String ediText, String sourceRef) {
        if (ediText == null || ediText.isBlank()) return List.of();

        String[] segments = ediText.split("~\\s*");
        List<Claim> claims = new ArrayList<>();
        Claim current = null;

        // Track loop-level values that appear before CLM
        String loopPatientId = null;
        String loopPayerId = null;

        for (String raw : segments) {
            String seg = raw.trim();
            if (seg.isEmpty()) continue;

            String[] el = seg.split("\\*", -1);
            String tag = el[0];

            // Capture subscriber/patient and payer at loop level (before CLM)
            if ("NM1".equals(tag)) {
                String entity = el.length > 1 ? el[1] : "";
                String id = el.length > 9 ? el[9] : null;

                switch (entity) {
                    // IL = subscriber (insured), QC = patient — treat both as patient
                    case "IL", "QC" -> {
                        if (id != null && !id.isBlank()) loopPatientId = id;
                    }
                    // PR = payer
                    case "PR" -> {
                        if (id != null && !id.isBlank()) loopPayerId = id;
                    }
                    // 82 = rendering provider — apply to current claim if exists
                    case "82" -> {
                        if (id != null && !id.isBlank() && current != null) {
                            current.setProviderId(id);
                        }
                    }
                    default -> { }
                }

                // Also apply to current claim if already started
                if (current != null) {
                    switch (entity) {
                        case "IL", "QC" -> {
                            if (id != null && !id.isBlank()) current.setPatientId(id);
                        }
                        case "PR" -> {
                            if (id != null && !id.isBlank()) current.setPayerId(id);
                        }
                        default -> { }
                    }
                }
                continue;
            }

            if ("CLM".equals(tag)) {
                current = new Claim();
                if (el.length > 1) current.setClaimId(el[1]);
                if (el.length > 2 && !el[2].isBlank()) {
                    try {
                        current.setBilledAmount(new BigDecimal(el[2]));
                    } catch (NumberFormatException ignored) { }
                }
                current.setEdiType("837");
                current.setClaimStatus("RECEIVED");
                current.setRawEdiReference(sourceRef);

                // Apply loop-level values captured before this CLM
                if (loopPatientId != null) current.setPatientId(loopPatientId);
                if (loopPayerId != null) current.setPayerId(loopPayerId);

                claims.add(current);
                continue;
            }

            if (current == null) continue;

            if ("DTP".equals(tag)) {
                // 472 = service date, 434 = statement date — accept either
                if (el.length > 3 && ("472".equals(el[1]) || "434".equals(el[1])) && "D8".equals(el[2])) {
                    if (current.getServiceDate() == null) {
                        try {
                            current.setServiceDate(LocalDate.parse(el[3], YYYYMMDD));
                        } catch (Exception ignored) { }
                    }
                }
            } else if ("SV1".equals(tag)) {
                if (el.length > 1 && el[1] != null) {
                    String comp = el[1];
                    int idx = comp.indexOf(':');
                    // Only set first procedure code found
                    if (current.getProcedureCode() == null) {
                        current.setProcedureCode(idx >= 0 ? comp.substring(idx + 1) : comp);
                    }
                }
            } else if ("HI".equals(tag)) {
                if (el.length > 1 && el[1] != null) {
                    String dx = el[1];
                    int idx = dx.indexOf(':');
                    if (current.getDiagnosisCode() == null) {
                        current.setDiagnosisCode(idx >= 0 ? dx.substring(idx + 1) : dx);
                    }
                }
            }
        }

        return claims;
    }
}
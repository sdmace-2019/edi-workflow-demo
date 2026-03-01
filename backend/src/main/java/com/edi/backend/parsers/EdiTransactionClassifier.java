package com.edi.backend.parsers;

import java.util.Optional;

public final class EdiTransactionClassifier {
    private EdiTransactionClassifier() {}

    public static Optional<String> detectTransactionSetId(String ediText) {
        if (ediText == null || ediText.isBlank()) return Optional.empty();

        String[] segments = ediText.split("~\\s*");
        for (String seg : segments) {
            String s = seg.trim();
            if (s.startsWith("ST*")) {
                String[] el = s.split("\\*");
                if (el.length >= 2 && !el[1].isBlank()) return Optional.of(el[1].trim());
            }
        }
        return Optional.empty();
    }
}
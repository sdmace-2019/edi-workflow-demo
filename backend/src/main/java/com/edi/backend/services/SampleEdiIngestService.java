package com.edi.backend.services;

import com.edi.backend.config.EdiIntakeProperties;
import com.edi.backend.models.Claim;
import com.edi.backend.parsers.Edi837Parser;
import com.edi.backend.parsers.EdiTransactionClassifier;
import com.edi.backend.repositories.ClaimRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class SampleEdiIngestService {

    public record FileResult(String fileName, String transactionSet, int claimsSaved, int claimsSkipped, String message) {}
    public record IngestResult(String resolvedDirectory, int filesProcessed, int totalClaimsSaved, List<FileResult> results) {}

    private final EdiIntakeProperties props;
    private final Edi837Parser edi837Parser;
    private final ClaimRepository claimRepository;

    public SampleEdiIngestService(EdiIntakeProperties props, Edi837Parser edi837Parser, ClaimRepository claimRepository) {
        this.props = props;
        this.edi837Parser = edi837Parser;
        this.claimRepository = claimRepository;
    }

    @Transactional
    public IngestResult ingestSamples() throws Exception {
        Path dir = Paths.get(props.getSampleDir()).toAbsolutePath().normalize();
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Sample dir not found or not a directory: " + dir);
        }

        List<FileResult> results = new ArrayList<>();
        int totalSaved = 0;

        try (var stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".edi") || n.endsWith(".txt") || n.endsWith(".x12");
                    })
                    .sorted()
                    .toList();

            for (Path file : files) {
                String fileName = file.getFileName().toString();
                String edi = Files.readString(file, StandardCharsets.UTF_8);

                String tx = EdiTransactionClassifier.detectTransactionSetId(edi).orElse("UNKNOWN");

                if ("835".equals(tx)) {
                    results.add(new FileResult(fileName, tx, 0, 0, "Skipped (835 not implemented yet)"));
                    continue;
                }
                if (!"837".equals(tx)) {
                    results.add(new FileResult(fileName, tx, 0, 0, "Skipped (unsupported transaction set)"));
                    continue;
                }

                List<Claim> claims = edi837Parser.parseClaims(edi, fileName);

                int saved = 0;
                int skipped = 0;

                for (Claim c : claims) {
                    boolean missingRequired =
                            isBlank(c.getClaimId()) ||
                            isBlank(c.getPatientId()) ||
                            isBlank(c.getProviderId()) ||
                            isBlank(c.getPayerId()) ||
                            c.getServiceDate() == null ||
                            isBlank(c.getProcedureCode()) ||
                            isBlank(c.getDiagnosisCode()) ||
                            c.getBilledAmount() == null;

                    if (missingRequired) { skipped++; continue; }
                    if (claimRepository.existsByClaimId(c.getClaimId())) { skipped++; continue; }

                    claimRepository.save(c);
                    saved++;
                }

                totalSaved += saved;
                results.add(new FileResult(fileName, tx, saved, skipped, "OK"));
            }

            return new IngestResult(dir.toString(), files.size(), totalSaved, results);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
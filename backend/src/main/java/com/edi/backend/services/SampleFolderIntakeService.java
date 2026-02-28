package com.edi.backend.services;

import com.edi.backend.config.EdiIntakeProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

@Service
public class SampleFolderIntakeService {

    public record SampleFile(String name, long sizeBytes) {}
    public record SampleIntakeResult(String resolvedDirectory, int fileCount, List<SampleFile> files) {}

    private final EdiIntakeProperties props;

    public SampleFolderIntakeService(EdiIntakeProperties props) {
        this.props = props;
    }

    public SampleIntakeResult scanSampleFolder() throws IOException {
        Path dir = Paths.get(props.getSampleDir()).toAbsolutePath().normalize();

        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Sample dir not found or not a directory: " + dir);
        }

        try (var stream = Files.list(dir)) {
            List<SampleFile> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".edi") || n.endsWith(".txt") || n.endsWith(".x12");
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .map(p -> {
                        try {
                            return new SampleFile(p.getFileName().toString(), Files.size(p));
                        } catch (IOException e) {
                            return new SampleFile(p.getFileName().toString(), -1);
                        }
                    })
                    .toList();

            return new SampleIntakeResult(dir.toString(), files.size(), files);
        }
    }
}

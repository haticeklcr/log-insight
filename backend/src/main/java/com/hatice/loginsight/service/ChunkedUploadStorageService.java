package com.hatice.loginsight.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkedUploadStorageService {

    private final Path baseDirectory;

    public ChunkedUploadStorageService(@Value("${app.upload.directory}") String uploadDirectory) {
        this.baseDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(baseDirectory);
    }

    public void createSessionDirectory(UUID uploadId) {
        try {
            Files.createDirectories(resolvePartsDir(uploadId));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path resolveSessionDir(UUID uploadId) {
        Path resolved = baseDirectory.resolve(uploadId.toString()).normalize();
        if (!resolved.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("Geçersiz uploadId: dizin izin verilen kök dışında");
        }
        return resolved;
    }

    public Path resolvePartsDir(UUID uploadId) {
        return resolveSessionDir(uploadId).resolve("parts");
    }

    public Path resolvePartFile(UUID uploadId, long chunkIndex) {
        return resolvePartsDir(uploadId).resolve(String.format("%06d.part", chunkIndex));
    }

    public Path resolveMergedFile(UUID uploadId) {
        return resolveSessionDir(uploadId).resolve("data.log");
    }

    public void writePart(UUID uploadId, long chunkIndex, byte[] content) {
        try {
            Files.write(resolvePartFile(uploadId, chunkIndex), content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Long> findMissingPartIndices(UUID uploadId, long totalChunks) {
        List<Long> missing = new ArrayList<>();
        for (long i = 0; i < totalChunks; i++) {
            if (!Files.exists(resolvePartFile(uploadId, i))) {
                missing.add(i);
            }
        }
        return missing;
    }

    public void deleteSessionDirectory(UUID uploadId) {
        Path dir = resolveSessionDir(uploadId);
        if (!Files.exists(dir)) {
            return;
        }
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
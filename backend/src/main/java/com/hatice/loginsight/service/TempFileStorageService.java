package com.hatice.loginsight.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TempFileStorageService {

    private final Path baseDirectory;

    public TempFileStorageService(@Value("${app.analysis-job.temp-directory}") String tempDirectory) {
        this.baseDirectory = Path.of(tempDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(baseDirectory);
    }

    public Path store(UUID jobId, MultipartFile file) throws IOException {
        Path targetPath = resolve(jobId);
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetPath;
    }

    public Path resolve(UUID jobId) {
        Path resolved = baseDirectory.resolve(jobId + ".tmp").normalize();
        if (!resolved.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("Geçersiz job ID: geçici dosya yolu izin verilen dizin dışında");
        }
        return resolved;
    }

    public void delete(UUID jobId) {
        try {
            Files.deleteIfExists(resolve(jobId));
        } catch (IOException e) {
            throw new UncheckedIoStorageException("Geçici dosya silinirken hata oluştu: " + jobId, e);
        }
    }

    public boolean exists(UUID jobId) {
        return Files.exists(resolve(jobId));
    }

    public List<UUID> listStoredJobIds() {
        try (var files = Files.list(baseDirectory)) {
            return files
                    .map(path -> path.getFileName().toString())
                    .map(this::parseJobIdFromFileName)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException("Geçici dizin listelenirken hata oluştu: " + baseDirectory, e);
        }
    }

    private Optional<UUID> parseJobIdFromFileName(String fileName) {
        if (!fileName.endsWith(".tmp")) {
            return Optional.empty();
        }
        String idPart = fileName.substring(0, fileName.length() - ".tmp".length());
        try {
            return Optional.of(UUID.fromString(idPart));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static class UncheckedIoStorageException extends RuntimeException {
        UncheckedIoStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
package com.hatice.loginsight.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public boolean mergedFileExists(UUID uploadId) {
        return Files.exists(resolveMergedFile(uploadId));
    }

    public int deleteStrayMergeTempFiles(UUID uploadId) {
        Path sessionDir = resolveSessionDir(uploadId);
        if (!Files.exists(sessionDir)) {
            return 0;
        }
        try (var files = Files.list(sessionDir)) {
            List<Path> strayFiles = files.filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("data.log") && name.endsWith(".tmp");
                    })
                    .toList();
            for (Path stray : strayFiles) {
                Files.deleteIfExists(stray);
            }
            return strayFiles.size();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ChunkWriteResult writePart(UUID uploadId, long chunkIndex, byte[] content) {
        Path target = resolvePartFile(uploadId, chunkIndex);

        if (Files.exists(target)) {
            return compareToExisting(target, content.length);
        }

        Path tempFile = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.write(tempFile, content);
            Files.move(tempFile, target, StandardCopyOption.ATOMIC_MOVE);
            return ChunkWriteResult.WRITTEN;
        } catch (FileAlreadyExistsException e) {
            deleteQuietly(tempFile);
            return compareToExisting(target, content.length);
        } catch (IOException e) {
            deleteQuietly(tempFile);
            throw new UncheckedIOException(e);
        }
    }

    private ChunkWriteResult compareToExisting(Path target, int incomingSize) {
        try {
            long existingSize = Files.size(target);
            return existingSize == incomingSize
                    ? ChunkWriteResult.ALREADY_EXISTS_SAME_SIZE
                    : ChunkWriteResult.ALREADY_EXISTS_DIFFERENT_SIZE;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // en iyi çaba: yarışı kaybeden isteğin kendi geçici dosyasını temizlemesi, kritik değil
        }
    }

    public enum ChunkWriteResult {
        WRITTEN,
        ALREADY_EXISTS_SAME_SIZE,
        ALREADY_EXISTS_DIFFERENT_SIZE
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

    public List<UUID> listSessionDirectoryIds() {
        try (var dirs = Files.list(baseDirectory)) {
            return dirs.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .map(this::parseUuid)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Optional<UUID> parseUuid(String name) {
        try {
            return Optional.of(UUID.fromString(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long getUsableSpace() {
        try {
            return Files.getFileStore(baseDirectory).getUsableSpace();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public long computeUploadedBytes(UUID uploadId) {
        Path partsDir = resolvePartsDir(uploadId);
        if (!Files.exists(partsDir)) {
            return 0L;
        }
        try (var files = Files.list(partsDir)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".part"))
                    .mapToLong(this::sizeQuietly)
                    .sum();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public long computeMergedBytes(UUID uploadId) {
        Path sessionDir = resolveSessionDir(uploadId);
        if (!Files.exists(sessionDir)) {
            return 0L;
        }
        try (var files = Files.list(sessionDir)) {
            return files.filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("data.log") && name.endsWith(".tmp");
                    })
                    .mapToLong(this::sizeQuietly)
                    .sum();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private long sizeQuietly(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    public void deletePartsDirectory(UUID uploadId) {
        Path partsDir = resolvePartsDir(uploadId);
        if (!Files.exists(partsDir)) {
            return;
        }
        try (var walk = Files.walk(partsDir)) {
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
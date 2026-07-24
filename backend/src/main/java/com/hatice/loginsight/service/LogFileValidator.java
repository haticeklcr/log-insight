package com.hatice.loginsight.service;

import com.hatice.loginsight.exception.EmptyFileException;
import com.hatice.loginsight.exception.FileTooLargeException;
import com.hatice.loginsight.exception.UnsupportedFileTypeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class LogFileValidator {

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".log", ".txt");

    private final DataSize maxFileSize;

    public LogFileValidator(@Value("${app.log-analysis.max-file-size:10MB}") DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EmptyFileException("Yüklenen dosya boş");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || ALLOWED_EXTENSIONS.stream().noneMatch(ext -> filename.toLowerCase().endsWith(ext))) {
            throw new UnsupportedFileTypeException("Sadece .log ve .txt uzantılı dosyalar desteklenir");
        }

        if (file.getSize() > maxFileSize.toBytes()) {
            throw new FileTooLargeException(
                    "Dosya boyutu izin verilen maksimum sınırı (" + maxFileSize.toMegabytes() + "MB) aşıyor");
        }
    }
}
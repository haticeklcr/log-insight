package com.hatice.loginsight.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ByteOffsetLineReaderTest {

    @TempDir
    Path tempDir;

    private Path writeFile(String name, byte[] content) throws IOException {
        Path path = tempDir.resolve(name);
        Files.write(path, content);
        return path;
    }

    private List<ByteOffsetLineReader.Line> readAll(Path path) throws IOException {
        return readAll(path, 0L, 64 * 1024);
    }

    private List<ByteOffsetLineReader.Line> readAll(Path path, long startOffset, int bufferSize) throws IOException {
        List<ByteOffsetLineReader.Line> lines = new ArrayList<>();
        try (ByteOffsetLineReader reader = new ByteOffsetLineReader(path, startOffset, bufferSize)) {
            ByteOffsetLineReader.Line line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    @Test
    void readsFileWithOnlyLfLineEndings() throws IOException {
        Path path = writeFile("lf.log", "birinci\nikinci\nucuncu\n".getBytes(StandardCharsets.UTF_8));

        List<ByteOffsetLineReader.Line> lines = readAll(path);

        assertThat(lines).hasSize(3);
        assertThat(lines.get(0).getContent()).isEqualTo("birinci");
        assertThat(lines.get(0).getStartOffset()).isZero();
        assertThat(lines.get(1).getContent()).isEqualTo("ikinci");
        assertThat(lines.get(1).getStartOffset()).isEqualTo(8);
        assertThat(lines.get(2).getContent()).isEqualTo("ucuncu");
        assertThat(lines.get(2).getStartOffset()).isEqualTo(15);
    }

    @Test
    void readsFileWithCrlfLineEndingsAndStripsCrFromContent() throws IOException {
        Path path = writeFile("crlf.log", "birinci\r\nikinci\r\n".getBytes(StandardCharsets.UTF_8));

        List<ByteOffsetLineReader.Line> lines = readAll(path);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).getContent()).isEqualTo("birinci");
        assertThat(lines.get(1).getContent()).isEqualTo("ikinci");
        // \r\n dahil 9 byte sonra ikinci satir basliyor
        assertThat(lines.get(1).getStartOffset()).isEqualTo(9);
    }

    @Test
    void handlesMultiByteUtf8CharactersUsingByteLengthNotCharLength() throws IOException {
        // "şğüöç" - Turkce coklu-byte karakterler, UTF-8'de her biri 2 byte
        String firstLine = "şğüöç";
        Path path = writeFile("utf8.log", (firstLine + "\nsonraki\n").getBytes(StandardCharsets.UTF_8));

        List<ByteOffsetLineReader.Line> lines = readAll(path);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).getContent()).isEqualTo(firstLine);
        int firstLineByteLength = firstLine.getBytes(StandardCharsets.UTF_8).length;
        assertThat(firstLineByteLength).isEqualTo(10); // 5 karakter * 2 byte
        assertThat(lines.get(1).getStartOffset()).isEqualTo(firstLineByteLength + 1); // +1 = \n
        assertThat(lines.get(1).getContent()).isEqualTo("sonraki");
    }

    @Test
    void handlesLineLongerThanReadBuffer() throws IOException {
        String longLine = "a".repeat(100);
        Path path = writeFile("long.log", (longLine + "\nkisa\n").getBytes(StandardCharsets.UTF_8));

        // Kasıtlı olarak çok küçük bir tampon (8 byte) kullanarak satırın tampon
        // sınırını birden çok kez aşmasını zorluyoruz.
        List<ByteOffsetLineReader.Line> lines = readAll(path, 0L, 8);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).getContent()).isEqualTo(longLine);
        assertThat(lines.get(1).getContent()).isEqualTo("kisa");
    }

    @Test
    void lastLineWithoutTrailingNewlineIsStillReturned() throws IOException {
        Path path = writeFile("no-trailing-newline.log", "birinci\nsonuncu".getBytes(StandardCharsets.UTF_8));

        List<ByteOffsetLineReader.Line> lines = readAll(path);

        assertThat(lines).hasSize(2);
        assertThat(lines.get(1).getContent()).isEqualTo("sonuncu");
    }

    @Test
    void emptyFileProducesNoLines() throws IOException {
        Path path = writeFile("empty.log", new byte[0]);

        List<ByteOffsetLineReader.Line> lines = readAll(path);

        assertThat(lines).isEmpty();
    }

    @Test
    void readingFromGivenOffsetProducesSameRemainingLinesAsReadingFromStart() throws IOException {
        Path path = writeFile("resume.log",
                "birinci\nikinci\nucuncu\ndorduncu\nbesinci\n".getBytes(StandardCharsets.UTF_8));

        List<ByteOffsetLineReader.Line> fullRead = readAll(path);
        long thirdLineOffset = fullRead.get(2).getStartOffset();

        List<ByteOffsetLineReader.Line> resumedRead = readAll(path, thirdLineOffset, 64 * 1024);

        assertThat(resumedRead).hasSize(3);
        assertThat(resumedRead.get(0).getContent()).isEqualTo("ucuncu");
        assertThat(resumedRead.get(0).getStartOffset()).isEqualTo(thirdLineOffset);
        assertThat(resumedRead.get(1).getContent()).isEqualTo("dorduncu");
        assertThat(resumedRead.get(2).getContent()).isEqualTo("besinci");
    }
}
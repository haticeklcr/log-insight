package com.hatice.loginsight.service;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ByteOffsetLineReader implements Closeable {

    private static final byte LF = (byte) '\n';
    private static final byte CR = (byte) '\r';
    private static final int DEFAULT_READ_BUFFER_SIZE = 64 * 1024;

    public static final class Line {

        private final String content;
        private final long startOffset;

        public Line(String content, long startOffset) {
            this.content = content;
            this.startOffset = startOffset;
        }

        public String getContent() {
            return content;
        }

        public long getStartOffset() {
            return startOffset;
        }
    }

    private final FileChannel channel;
    private final ByteBuffer readBuffer;
    private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream(256);
    private long currentPosition;
    private boolean finished = false;

    public ByteOffsetLineReader(Path filePath) throws IOException {
        this(filePath, 0L, DEFAULT_READ_BUFFER_SIZE);
    }

    public ByteOffsetLineReader(Path filePath, long startOffset) throws IOException {
        this(filePath, startOffset, DEFAULT_READ_BUFFER_SIZE);
    }

    public ByteOffsetLineReader(Path filePath, long startOffset, int readBufferSize) throws IOException {
        this.channel = FileChannel.open(filePath, StandardOpenOption.READ);
        this.channel.position(startOffset);
        this.currentPosition = startOffset;
        this.readBuffer = ByteBuffer.allocate(readBufferSize);
        this.readBuffer.limit(0);
    }

    public Line readLine() throws IOException {
        if (finished) {
            return null;
        }

        lineBuffer.reset();
        long thisLineStart = currentPosition;
        boolean readAnyByte = false;

        while (true) {
            if (!readBuffer.hasRemaining()) {
                readBuffer.clear();
                int readCount = channel.read(readBuffer);
                readBuffer.flip();
                if (readCount == -1) {
                    finished = true;
                    break;
                }
            }

            byte b = readBuffer.get();
            currentPosition++;
            readAnyByte = true;

            if (b == LF) {
                break;
            }
            lineBuffer.write(b);
        }

        if (!readAnyByte) {
            return null;
        }

        byte[] rawLineBytes = lineBuffer.toByteArray();
        int contentLength = rawLineBytes.length;
        if (contentLength > 0 && rawLineBytes[contentLength - 1] == CR) {
            contentLength--;
        }
        String content = new String(rawLineBytes, 0, contentLength, StandardCharsets.UTF_8);
        return new Line(content, thisLineStart);
    }

    public long getCurrentPosition() {
        return currentPosition;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
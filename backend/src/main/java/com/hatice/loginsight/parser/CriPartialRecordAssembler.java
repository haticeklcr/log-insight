package com.hatice.loginsight.parser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CriPartialRecordAssembler {

    private final int maxRecordLength;

    private String openStream;
    private StringBuilder openBuffer;
    private Instant openTimestamp;
    private boolean openTruncated;

    public CriPartialRecordAssembler(int maxRecordLength) {
        this.maxRecordLength = maxRecordLength;
    }

    public List<AssembledCriRecord> offer(String stream, String partialTag, String payload, Instant fragmentTimestamp) {
        List<AssembledCriRecord> results = new ArrayList<>();

        boolean validCriFragment = stream != null && partialTag != null;
        if (!validCriFragment) {
            closeOpenBufferAsIncomplete(results);
            results.add(new AssembledCriRecord(payload, fragmentTimestamp, false, false));
            return results;
        }

        if (openStream != null && !openStream.equals(stream)) {
            closeOpenBufferAsIncomplete(results);
        }

        if ("F".equals(partialTag)) {
            if (openStream == null) {
                results.add(new AssembledCriRecord(payload, fragmentTimestamp, false, false));
            } else {
                appendToOpenBuffer(payload);
                results.add(new AssembledCriRecord(openBuffer.toString(), openTimestamp, openTruncated, false));
                resetOpenBuffer();
            }
            return results;
        }

        if (openStream == null) {
            openStream = stream;
            openBuffer = new StringBuilder();
            openTimestamp = fragmentTimestamp;
            openTruncated = false;
        }
        appendToOpenBuffer(payload);
        return results;
    }

    public List<AssembledCriRecord> flush() {
        List<AssembledCriRecord> results = new ArrayList<>();
        closeOpenBufferAsIncomplete(results);
        return results;
    }

    private void closeOpenBufferAsIncomplete(List<AssembledCriRecord> results) {
        if (openStream != null) {
            results.add(new AssembledCriRecord(openBuffer.toString(), openTimestamp, openTruncated, true));
            resetOpenBuffer();
        }
    }

    private void appendToOpenBuffer(String payload) {
        if (openBuffer.length() >= maxRecordLength) {
            openTruncated = true;
            return;
        }
        int remaining = maxRecordLength - openBuffer.length();
        if (payload.length() > remaining) {
            openBuffer.append(payload, 0, remaining);
            openTruncated = true;
        } else {
            openBuffer.append(payload);
        }
    }

    private void resetOpenBuffer() {
        openStream = null;
        openBuffer = null;
        openTimestamp = null;
        openTruncated = false;
    }
}
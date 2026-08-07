package com.hatice.loginsight.parser;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnvelopeDetectorTest {

    private final EnvelopeDetector detector = new EnvelopeDetector(true, 80);

    @Test
    void detectsSyslogRfc3164Envelope() {
        List<String> sampleLines = List.of(
                "Jul 30 06:55:07 dc05 java[2172]: 2026-07-30 06:55:07.702 INFO 2172 --- [main] com.example.Foo : mesaj bir",
                "Jul 30 06:55:08 dc05 java[2172]: 2026-07-30 06:55:08.104 ERROR 2172 --- [main] com.example.Foo : mesaj iki");

        LogEnvelopeDetectionResult result = detector.detect(sampleLines);

        assertEquals(LogEnvelope.SYSLOG_RFC3164, result.getDetectedEnvelope());
        assertEquals(100, result.getConfidence());
    }

    @Test
    void detectsSyslogRfc5424Envelope() {
        List<String> sampleLines = List.of(
                "<134>1 2026-07-30T06:55:07.702+03:00 dc05 java 2172 - - mesaj bir",
                "<134>1 2026-07-30T06:55:08.104+03:00 dc05 java 2172 - - mesaj iki");

        LogEnvelopeDetectionResult result = detector.detect(sampleLines);

        assertEquals(LogEnvelope.SYSLOG_RFC5424, result.getDetectedEnvelope());
        assertEquals(100, result.getConfidence());
    }

    @Test
    void detectsContainerCriEnvelope() {
        List<String> sampleLines = List.of(
                "2026-07-30T06:55:07.700Z stdout F mesaj bir",
                "2026-07-30T06:55:08.100Z stdout F mesaj iki");

        LogEnvelopeDetectionResult result = detector.detect(sampleLines);

        assertEquals(LogEnvelope.CONTAINER_CRI, result.getDetectedEnvelope());
        assertEquals(100, result.getConfidence());
    }

    @Test
    void returnsNoneWhenNoEnvelopePresent() {
        List<String> sampleLines = List.of(
                "2026-07-30 06:55:07.702 INFO 2172 --- [main] com.example.Foo : mesaj bir",
                "2026-07-30 06:55:08.104 ERROR 2172 --- [main] com.example.Foo : mesaj iki");

        LogEnvelopeDetectionResult result = detector.detect(sampleLines);

        assertEquals(LogEnvelope.NONE, result.getDetectedEnvelope());
    }

    @Test
    void doesNotApplyEnvelopeWhenBelowConfidenceThreshold() {
        List<String> sampleLines = List.of(
                "Jul 30 06:55:07 dc05 java[2172]: mesaj bir",
                "duz metin satiri, envelope degil",
                "baska duz metin satiri",
                "bir tane daha duz metin satiri",
                "ve bir tane daha");

        LogEnvelopeDetectionResult result = detector.detect(sampleLines);

        assertEquals(LogEnvelope.NONE, result.getDetectedEnvelope());
    }

    @Test
    void canBeDisabledViaConfiguration() {
        EnvelopeDetector disabledDetector = new EnvelopeDetector(false, 80);
        List<String> sampleLines = List.of(
                "Jul 30 06:55:07 dc05 java[2172]: mesaj bir",
                "Jul 30 06:55:08 dc05 java[2172]: mesaj iki");

        LogEnvelopeDetectionResult result = disabledDetector.detect(sampleLines);

        assertEquals(LogEnvelope.NONE, result.getDetectedEnvelope());
    }

    @Test
    void stripsRfc3164PayloadAndTimestamp() {
        EnvelopeStripResult result = detector.strip(
                "Jul 30 06:55:07 dc05 java[2172]: 2026-07-30 06:55:07.702 INFO gercek mesaj",
                LogEnvelope.SYSLOG_RFC3164);

        assertNotNull(result);
        assertEquals("2026-07-30 06:55:07.702 INFO gercek mesaj", result.getPayload());
        assertNotNull(result.getEnvelopeTimestamp());

        ZonedDateTime zoned = ZonedDateTime.ofInstant(result.getEnvelopeTimestamp(), ZoneId.systemDefault());
        assertEquals(7, zoned.getMonthValue());
        assertEquals(30, zoned.getDayOfMonth());
        assertEquals(6, zoned.getHour());
        assertEquals(55, zoned.getMinute());
        assertEquals(7, zoned.getSecond());
    }

    @Test
    void stripsRfc5424PayloadAndTimestamp() {
        EnvelopeStripResult result = detector.strip(
                "<134>1 2026-07-30T06:55:07.702+03:00 dc05 java 2172 - - gercek mesaj",
                LogEnvelope.SYSLOG_RFC5424);

        assertNotNull(result);
        assertEquals("gercek mesaj", result.getPayload());
        assertEquals(Instant.parse("2026-07-30T03:55:07.702Z"), result.getEnvelopeTimestamp());
    }

    @Test
    void stripsCriPayloadStreamAndPartialTag() {
        EnvelopeStripResult result = detector.strip(
                "2026-07-30T06:55:07.700Z stdout P gercek mesaj",
                LogEnvelope.CONTAINER_CRI);

        assertNotNull(result);
        assertEquals("gercek mesaj", result.getPayload());
        assertEquals("stdout", result.getCriStream());
        assertEquals("P", result.getCriPartialTag());
        assertEquals(Instant.parse("2026-07-30T06:55:07.700Z"), result.getEnvelopeTimestamp());
    }

    @Test
    void stripReturnsNullWhenLineDoesNotMatchEnvelope() {
        EnvelopeStripResult result = detector.strip("boyle bir onek yok", LogEnvelope.SYSLOG_RFC3164);

        assertNull(result);
    }
}
package com.hatice.loginsight.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveDataMaskerTest {

    private final SensitiveDataMasker masker = new SensitiveDataMasker();

    @Test
    void masksAuthorizationBearerToken() {
        String result = masker.mask("Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.abc.def");
        assertEquals("Authorization: Bearer ****", result);
    }

    @Test
    void masksBareBearerToken() {
        String result = masker.mask("token used was Bearer abc123xyz");
        assertTrue(result.contains("Bearer ****"));
        assertFalse(result.contains("abc123xyz"));
    }

    @Test
    void masksEmailAddress() {
        String result = masker.mask("Login attempt from hatice@example.com failed");
        assertFalse(result.contains("hatice@example.com"));
        assertTrue(result.contains("****"));
    }

    @Test
    void masksPasswordField() {
        String result = masker.mask("password=SuperSecret123");
        assertFalse(result.contains("SuperSecret123"));
        assertTrue(result.toLowerCase().contains("password"));
    }

    @Test
    void masksApiKeyField() {
        String result = masker.mask("api_key=sk-1234567890abcdef");
        assertFalse(result.contains("sk-1234567890abcdef"));
    }

    @Test
    void masksCreditCardLikeNumber() {
        String result = masker.mask("Card number 4111 1111 1111 1111 charged");
        assertFalse(result.contains("4111 1111 1111 1111"));
    }

    @Test
    void leavesNonSensitiveMessageUnchanged() {
        String message = "Cache refreshed successfully";
        assertEquals(message, masker.mask(message));
    }

    @Test
    void returnsNullForNullInput() {
        assertNull(masker.mask(null));
    }
}
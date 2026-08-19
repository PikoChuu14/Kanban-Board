package com.company.kanban.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void acceptsBase64SecretThatDecodesTo256Bits() {
        String secret = Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)
        );

        assertDoesNotThrow(() -> new JwtService(secret, "base64", 60_000));
    }

    @Test
    void rejectsBase64SecretShorterThan256BitsWithClearMessage() {
        String secret = Base64.getEncoder().encodeToString(new byte[31]);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new JwtService(secret, "base64", 60_000)
        );

        assertEquals(
                "app.jwt.secret must decode to at least 32 bytes for HS256.",
                exception.getMessage()
        );
    }

    @Test
    void acceptsValidLegacyRawSecretWithoutChangingItsInterpretation() {
        assertDoesNotThrow(() -> new JwtService(
                "legacy-production-secret-32-bytes!",
                "raw",
                60_000
        ));
    }
}

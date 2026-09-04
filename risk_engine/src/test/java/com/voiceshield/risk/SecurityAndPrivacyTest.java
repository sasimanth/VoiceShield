package com.voiceshield.risk;

import com.voiceshield.risk.security.PrivacyGuard;
import com.voiceshield.risk.security.ReplayProtection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SecurityAndPrivacyTest {

    private PrivacyGuard privacyGuard;
    private ReplayProtection replayProtection;

    @BeforeEach
    void setUp() {
        privacyGuard = new PrivacyGuard();
        replayProtection = new ReplayProtection();
    }

    @Test
    @DisplayName("Should mask caller phone numbers to protect PII in logs")
    void testCallerIdMasking() {
        String rawNumber = "+91-9876543210";
        String masked = privacyGuard.maskCallerId(rawNumber);

        assertThat(masked).contains("******");
        assertThat(masked).doesNotContain("98765");
        assertThat(masked).endsWith("3210");
    }

    @Test
    @DisplayName("Should generate deterministic yet irreversible SHA-256 session pseudonyms")
    void testPseudonymGeneration() {
        String caller = "+91-9876543210";
        String timestamp = "2026-09-03T17:00:00Z";

        String hash1 = privacyGuard.generateAnonymizedSessionId(caller, timestamp);
        String hash2 = privacyGuard.generateAnonymizedSessionId(caller, timestamp);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(16);
        assertThat(hash1).doesNotContain(caller);
    }

    @Test
    @DisplayName("Should detect and reject replayed nonces")
    void testReplayNonceDetection() {
        String nonce = "unique_token_12345";
        String timestamp = Instant.now().toString();

        boolean firstCall = replayProtection.validate(nonce, timestamp);
        boolean secondCall = replayProtection.validate(nonce, timestamp);

        assertThat(firstCall).isTrue();
        assertThat(secondCall).isFalse(); // Replay rejected
    }

    @Test
    @DisplayName("Should reject requests with timestamps drifted beyond 5-minute sliding window")
    void testExpiredTimestampRejection() {
        String nonce = "token_fresh_999";
        String expiredTimestamp = Instant.now().minus(10, ChronoUnit.MINUTES).toString();

        boolean isValid = replayProtection.validate(nonce, expiredTimestamp);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should format zero-retention audit record conforming to DPDP Act 2023")
    void testZeroRetentionAuditLog() {
        Map<String, Object> audit = privacyGuard.createAuditRecord("hash_abc123", 85, "BLOCK");

        assertThat(audit.get("raw_audio_stored")).isEqualTo(false);
        assertThat(audit.get("retention_policy")).isEqualTo("ZERO_RETENTION_FEATURE_ONLY");
        assertThat(audit.get("compliance_status")).isEqualTo("DPDP_ACT_2023_COMPLIANT");
    }
}

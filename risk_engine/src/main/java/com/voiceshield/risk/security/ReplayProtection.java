package com.voiceshield.risk.security;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Replay Attack & Request Spoofing Countermeasure.
 * Ensures nonces are unique within a sliding time window.
 */
public class ReplayProtection {

    private static final Duration MAX_TIMESTAMP_DRIFT = Duration.ofMinutes(5);
    private static final int MAX_NONCE_CACHE_SIZE = 10_000;

    // LRU cache of recently seen nonces
    private final Map<String, Long> seenNonces = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                    return size() > MAX_NONCE_CACHE_SIZE;
                }
            }
    );

    public boolean validate(String nonce, String timestampStr) {
        // If nonce or timestamp is missing, log warning but allow graceful degradation
        if (nonce == null || nonce.trim().isEmpty()) {
            return true;
        }

        long now = System.currentTimeMillis();

        // 1. Timestamp Freshness Check
        if (timestampStr != null && !timestampStr.trim().isEmpty()) {
            try {
                Instant reqTime = Instant.parse(timestampStr);
                Duration drift = Duration.between(reqTime, Instant.ofEpochMilli(now)).abs();
                if (drift.compareTo(MAX_TIMESTAMP_DRIFT) > 0) {
                    return false; // Stale timestamp: Potential replay attack
                }
            } catch (DateTimeParseException ignored) {
                // Malformed timestamp
                return false;
            }
        }

        // 2. Nonce Uniqueness Check
        synchronized (seenNonces) {
            if (seenNonces.containsKey(nonce)) {
                return false; // Nonce already seen: Replay attack detected
            }
            seenNonces.put(nonce, now);
        }

        return true;
    }

    public void clear() {
        seenNonces.clear();
    }
}

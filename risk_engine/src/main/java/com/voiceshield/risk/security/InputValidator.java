package com.voiceshield.risk.security;

import com.voiceshield.risk.model.ContextMetadata;
import com.voiceshield.risk.model.RiskSignalInput;

/**
 * Defensive Input Validation and Sanitization.
 * Protects against Malicious inputs, Log Injection, NaN, Infinity, and Out-of-bounds payloads.
 */
public class InputValidator {

    public RiskSignalInput sanitize(RiskSignalInput raw) {
        if (raw == null) {
            return new RiskSignalInput();
        }

        RiskSignalInput clean = new RiskSignalInput();

        // 1. Sanitize Strings against CRLF / Log Injection
        clean.setSessionId(sanitizeString(raw.getSessionId(), 64));
        clean.setCallerId(sanitizeString(raw.getCallerId(), 32));
        clean.setTimestamp(sanitizeString(raw.getTimestamp(), 32));
        clean.setNonce(sanitizeString(raw.getNonce(), 64));

        // 2. Validate & Clamp Probabilities and Anomaly Scores (0.0 to 1.0)
        clean.setDeepfakeProbability(clampScore(raw.getDeepfakeProbability()));
        clean.setSpeakerSimilarity(clampScore(raw.getSpeakerSimilarity()));
        clean.setAcousticAnomaly(clampScore(raw.getAcousticAnomaly()));
        clean.setProsodicAnomaly(clampScore(raw.getProsodicAnomaly()));
        clean.setBehavioralAnomaly(clampScore(raw.getBehavioralAnomaly()));

        // 3. Speaker Verification Status
        clean.setSpeakerVerificationStatus(raw.getSpeakerVerificationStatus());

        // 4. Sanitize Context Metadata
        ContextMetadata rawCtx = raw.getContext();
        if (rawCtx != null) {
            ContextMetadata cleanCtx = new ContextMetadata();
            cleanCtx.setTransactionAmount(sanitizeAmount(rawCtx.getTransactionAmount()));
            cleanCtx.setCurrency(sanitizeCurrency(rawCtx.getCurrency()));
            cleanCtx.setNewBeneficiary(rawCtx.isNewBeneficiary());
            cleanCtx.setInternationalCall(rawCtx.isInternationalCall());
            cleanCtx.setUrgency(rawCtx.isUrgency());
            cleanCtx.setCallerTrustScore(clampScore(rawCtx.getCallerTrustScore()));
            cleanCtx.setPrivilegedAccount(rawCtx.isPrivilegedAccount());
            cleanCtx.setFailedAuthAttempts(Math.max(0, Math.min(10, rawCtx.getFailedAuthAttempts())));
            clean.setContext(cleanCtx);
        } else {
            clean.setContext(new ContextMetadata());
        }

        return clean;
    }

    private Double clampScore(Double score) {
        if (score == null) {
            return null;
        }
        if (Double.isNaN(score) || Double.isInfinite(score)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, score));
    }

    private Double sanitizeAmount(Double amount) {
        if (amount == null || Double.isNaN(amount) || Double.isInfinite(amount) || amount < 0) {
            return 0.0;
        }
        // Maximum cap to prevent overflow attacks (e.g. ₹100 Crores)
        return Math.min(1_000_000_000.0, amount);
    }

    private String sanitizeString(String val, int maxLen) {
        if (val == null) {
            return null;
        }
        // Remove line breaks and non-printable control characters
        String clean = val.replaceAll("[\\r\\n\\t]", "").trim();
        if (clean.length() > maxLen) {
            clean = clean.substring(0, maxLen);
        }
        return clean;
    }

    private String sanitizeCurrency(String currency) {
        if (currency == null) {
            return "INR";
        }
        String clean = currency.replaceAll("[^a-zA-Z]", "").toUpperCase();
        return clean.length() == 3 ? clean : "INR";
    }
}

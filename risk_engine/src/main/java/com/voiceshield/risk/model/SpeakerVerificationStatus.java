package com.voiceshield.risk.model;

/**
 * Speaker Biometric Verification Outcome.
 * Strictly separates Voice Authenticity from Speaker Identity.
 * If reference sample is absent, UNAVAILABLE must be used.
 */
public enum SpeakerVerificationStatus {
    MATCH,
    SUSPICIOUS_DEVIATION,
    MISMATCH,
    UNAVAILABLE
}

package com.voiceshield.risk.model;

/**
 * Standardized Actionable Security Decisions for VoiceShield.
 * Frozen in integration/contracts/risk_contract.schema.json
 */
public enum Decision {
    ALLOW,
    WARN,
    SECONDARY_VERIFICATION,
    BLOCK
}

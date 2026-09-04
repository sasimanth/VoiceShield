package com.voiceshield.risk.mock;

import com.voiceshield.risk.model.ContextMetadata;
import com.voiceshield.risk.model.RiskSignalInput;
import com.voiceshield.risk.model.SpeakerVerificationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Mock Signal Factory for Parallel Development.
 * Allows the Risk Engine to be tested thoroughly before Person 1 (ML)
 * and Person 2 (Speech Processing) complete their modules.
 */
public class MockSignalFactory {

    /**
     * Exact prompt example scenario:
     * deepfake_probability = 0.87, speaker_match = false, context_risk = 0.70
     */
    public static RiskSignalInput createPromptExampleAttack() {
        ContextMetadata ctx = new ContextMetadata();
        ctx.setTransactionAmount(500000.0); // 0.35
        ctx.setUrgency(true);                // 0.25
        ctx.setInternationalCall(true);      // 0.15 -> total context_risk = 0.75 >= 0.70

        RiskSignalInput input = new RiskSignalInput(
                0.87,
                SpeakerVerificationStatus.MISMATCH,
                0.25,
                ctx
        );
        input.setAcousticAnomaly(0.82);
        input.setProsodicAnomaly(0.79);
        input.setBehavioralAnomaly(0.65);
        populateStandardMetadata(input, "+91-9876543210");
        return input;
    }

    /**
     * Low-Risk Scenario: Genuine human customer, normal small transfer.
     */
    public static RiskSignalInput createLowRiskGenuineHuman() {
        ContextMetadata ctx = new ContextMetadata();
        ctx.setTransactionAmount(4500.0);
        ctx.setNewBeneficiary(false);
        ctx.setUrgency(false);
        ctx.setCallerTrustScore(0.95);

        RiskSignalInput input = new RiskSignalInput(
                0.04,
                SpeakerVerificationStatus.MATCH,
                0.97,
                ctx
        );
        input.setAcousticAnomaly(0.12);
        input.setProsodicAnomaly(0.15);
        populateStandardMetadata(input, "+91-9811122233");
        return input;
    }

    /**
     * Medium-Risk Scenario: Mild voice anomalies without critical context escalation.
     */
    public static RiskSignalInput createMediumRiskWarning() {
        ContextMetadata ctx = new ContextMetadata();
        ctx.setTransactionAmount(45000.0);
        ctx.setNewBeneficiary(false);
        ctx.setCallerTrustScore(0.85);

        RiskSignalInput input = new RiskSignalInput(
                0.35,
                SpeakerVerificationStatus.SUSPICIOUS_DEVIATION,
                0.62,
                ctx
        );
        input.setAcousticAnomaly(0.38);
        input.setProsodicAnomaly(0.40);
        populateStandardMetadata(input, "+91-9844455566");
        return input;
    }

    /**
     * High-Risk Scenario: Elevated deepfake likelihood, mismatch voice.
     */
    public static RiskSignalInput createHighRiskSecondaryAuth() {
        ContextMetadata ctx = new ContextMetadata();
        ctx.setTransactionAmount(300000.0);
        ctx.setUrgency(true);
        ctx.setCallerTrustScore(0.60);

        RiskSignalInput input = new RiskSignalInput(
                0.68,
                SpeakerVerificationStatus.MISMATCH,
                0.40,
                ctx
        );
        input.setAcousticAnomaly(0.55);
        input.setProsodicAnomaly(0.60);
        populateStandardMetadata(input, "+91-9877788899");
        return input;
    }

    /**
     * Contradictory Signals Scenario:
     * High deepfake probability (0.89) BUT Speaker Matches Enrolled Voice (0.94).
     * (E.g. Targeted high-fidelity voice cloning of a known CFO/Executive).
     */
    public static RiskSignalInput createContradictorySignalsTargetedClone() {
        ContextMetadata ctx = new ContextMetadata();
        ctx.setTransactionAmount(750000.0);
        ctx.setUrgency(true);
        ctx.setPrivilegedAccount(true);

        RiskSignalInput input = new RiskSignalInput(
                0.89,
                SpeakerVerificationStatus.MATCH,
                0.94,
                ctx
        );
        input.setAcousticAnomaly(0.78);
        input.setProsodicAnomaly(0.72);
        populateStandardMetadata(input, "+91-9999900000");
        return input;
    }

    /**
     * Missing Signals Scenario:
     * Speaker verification UNAVAILABLE (new or un-enrolled customer, similarity null).
     */
    public static RiskSignalInput createMissingSignalsUnenrolledCustomer() {
        ContextMetadata ctx = new ContextMetadata();
        ctx.setTransactionAmount(50000.0);
        ctx.setNewBeneficiary(false);

        RiskSignalInput input = new RiskSignalInput(
                0.22,
                SpeakerVerificationStatus.UNAVAILABLE,
                null,
                ctx
        );
        populateStandardMetadata(input, "+91-9700011122");
        return input;
    }

    private static void populateStandardMetadata(RiskSignalInput input, String callerId) {
        input.setSessionId("sess_" + UUID.randomUUID().toString().substring(0, 8));
        input.setCallerId(callerId);
        input.setTimestamp(Instant.now().toString());
        input.setNonce(UUID.randomUUID().toString());
    }
}

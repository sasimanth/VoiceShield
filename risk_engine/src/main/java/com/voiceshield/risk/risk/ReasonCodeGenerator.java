package com.voiceshield.risk.risk;

import com.voiceshield.risk.model.RiskSignalInput;
import com.voiceshield.risk.model.SpeakerVerificationStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic Reason Generator for VoiceShield.
 * Ensures SOC operators receive explainable forensic rationale for each decision.
 */
public class ReasonCodeGenerator {

    public List<String> generateReasons(RiskSignalInput input, double contextualRisk,
                                        List<String> contextThreatFactors, int riskScore) {
        List<String> reasons = new ArrayList<>();

        // 1. Deepfake / Synthetic Speech Reasons
        Double deepfakeProb = input.getDeepfakeProbability();
        if (deepfakeProb != null) {
            if (deepfakeProb >= 0.85) {
                reasons.add(String.format("Critical AI voice cloning detected (synthetic_prob: %.2f)", deepfakeProb));
            } else if (deepfakeProb >= 0.65) {
                reasons.add(String.format("Elevated synthetic speech probability detected (synthetic_prob: %.2f)", deepfakeProb));
            } else if (deepfakeProb <= 0.20 && riskScore < 25) {
                reasons.add("Voice verified as genuine human vocal tract resonance");
            }
        }

        // 2. Speaker Verification Reasons
        SpeakerVerificationStatus status = input.getSpeakerVerificationStatus();
        if (status == SpeakerVerificationStatus.MISMATCH) {
            Double sim = input.getSpeakerSimilarity();
            if (sim != null) {
                reasons.add(String.format("Speaker biometric mismatch against enrolled voice profile (similarity: %.2f)", sim));
            } else {
                reasons.add("Speaker biometric mismatch against enrolled voice profile");
            }
        } else if (status == SpeakerVerificationStatus.SUSPICIOUS_DEVIATION) {
            reasons.add("Speaker vocal tract features show suspicious deviation from enrolled reference");
        } else if (status == SpeakerVerificationStatus.UNAVAILABLE) {
            reasons.add("No enrolled voice profile found; speaker identity verification unavailable");
        } else if (status == SpeakerVerificationStatus.MATCH && (deepfakeProb != null && deepfakeProb >= 0.70)) {
            // Contradictory Signal Alert
            reasons.add("SECURITY ALERT: Speaker matches enrolled profile BUT audio exhibits synthetic neural vocoder artifacts (potential targeted voice clone)");
        }

        // 3. Acoustic and Prosodic Anomaly Reasons
        if (input.getAcousticAnomaly() != null && input.getAcousticAnomaly() >= 0.60) {
            reasons.add(String.format("Severe spectral flux / vocoder high-frequency anomaly (score: %.2f)", input.getAcousticAnomaly()));
        }
        if (input.getProsodicAnomaly() != null && input.getProsodicAnomaly() >= 0.65) {
            reasons.add(String.format("Monotone robotic pitch contour / prosodic cadence anomaly (score: %.2f)", input.getProsodicAnomaly()));
        }

        // 4. Contextual Threat Reasons (Passed from Context Analysis Stage)
        if (contextThreatFactors != null && !contextThreatFactors.isEmpty()) {
            reasons.addAll(contextThreatFactors);
        }

        // 5. Default Safe Reason if empty
        if (reasons.isEmpty()) {
            if (riskScore >= 50) {
                reasons.add("Aggregate multi-signal risk exceeded safe operating threshold");
            } else {
                reasons.add("All voice integrity biometrics within normal human parameters");
            }
        }

        return reasons;
    }
}

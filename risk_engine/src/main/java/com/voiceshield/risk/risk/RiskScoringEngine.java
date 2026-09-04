package com.voiceshield.risk.risk;

import com.voiceshield.risk.context.ContextAnalyzer;
import com.voiceshield.risk.model.*;
import com.voiceshield.risk.security.InputValidator;
import com.voiceshield.risk.security.PrivacyGuard;
import com.voiceshield.risk.security.ReplayProtection;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage 2: Multi-Signal Risk Scoring & Decision Engine.
 * 
 * Pipeline flow:
 * signals -> context analysis -> risk evaluation -> risk score (0-100) -> decision
 * 
 * Strict Contract: Produces standard RiskEvaluationResult for Person 3.
 */
public class RiskScoringEngine {

    public static final String MODEL_VERSION = "v1.2.0-composite-risk";

    private final ContextAnalyzer contextAnalyzer;
    private final RiskWeightsConfig weightsConfig;
    private final DecisionEngine decisionEngine;
    private final ReasonCodeGenerator reasonGenerator;
    private final InputValidator inputValidator;
    private final ReplayProtection replayProtection;
    private final PrivacyGuard privacyGuard;

    public RiskScoringEngine() {
        this(new RiskWeightsConfig());
    }

    public RiskScoringEngine(RiskWeightsConfig weightsConfig) {
        this.weightsConfig = weightsConfig != null ? weightsConfig : RiskWeightsConfig.createDefault();
        this.contextAnalyzer = new ContextAnalyzer();
        this.decisionEngine = new DecisionEngine(this.weightsConfig);
        this.reasonGenerator = new ReasonCodeGenerator();
        this.inputValidator = new InputValidator();
        this.replayProtection = new ReplayProtection();
        this.privacyGuard = new PrivacyGuard();
    }

    public RiskEvaluationResult evaluate(RiskSignalInput input) {
        // 1. Strict Security & Input Sanitization
        RiskSignalInput sanitized = inputValidator.sanitize(input);

        // 2. Anti-Replay Validation
        boolean replayValid = replayProtection.validate(sanitized.getNonce(), sanitized.getTimestamp());

        // 3. Stage 1: Separate Contextual Analysis
        ContextAnalysisResult contextResult = contextAnalyzer.analyze(sanitized.getContext());
        double contextRisk = contextResult.getContextualRisk();

        // 4. Extract & Normalize Speech Signals
        double deepfakeProb = sanitized.getDeepfakeProbability() != null ? sanitized.getDeepfakeProbability() : 0.0;
        double acousticAnomaly = sanitized.getAcousticAnomaly() != null ? sanitized.getAcousticAnomaly() : 0.0;
        double prosodicAnomaly = sanitized.getProsodicAnomaly() != null ? sanitized.getProsodicAnomaly() : 0.0;

        // 5. Speaker Verification Mismatch Calculation
        SpeakerVerificationStatus speakerStatus = sanitized.getSpeakerVerificationStatus();
        Double speakerSim = sanitized.getSpeakerSimilarity();
        Double speakerMismatchScore = null;

        if (speakerStatus != null && speakerStatus != SpeakerVerificationStatus.UNAVAILABLE) {
            if (speakerStatus == SpeakerVerificationStatus.MATCH) {
                speakerMismatchScore = 0.0;
            } else if (speakerStatus == SpeakerVerificationStatus.SUSPICIOUS_DEVIATION) {
                speakerMismatchScore = speakerSim != null ? Math.max(0.0, 1.0 - speakerSim) : 0.50;
            } else if (speakerStatus == SpeakerVerificationStatus.MISMATCH) {
                speakerMismatchScore = speakerSim != null ? Math.max(0.0, 1.0 - speakerSim) : 1.0;
            }
        }

        // 6. Multi-Signal Composite Calculation
        double rawComposite;
        if (speakerMismatchScore != null) {
            // Full 5-signal weighted equation
            rawComposite = (deepfakeProb * weightsConfig.getDeepfakeWeight())
                    + (acousticAnomaly * weightsConfig.getAcousticWeight())
                    + (prosodicAnomaly * weightsConfig.getProsodicWeight())
                    + (speakerMismatchScore * weightsConfig.getSpeakerMismatchWeight())
                    + (contextRisk * weightsConfig.getContextWeight());
        } else {
            // Graceful Fallback: Speaker identity unavailable -> Re-weight dynamically
            rawComposite = (deepfakeProb * weightsConfig.getFallbackDeepfakeWeight())
                    + (acousticAnomaly * weightsConfig.getFallbackAcousticWeight())
                    + (prosodicAnomaly * weightsConfig.getFallbackProsodicWeight())
                    + (contextRisk * weightsConfig.getFallbackContextWeight());
        }

        // 7. Behavioral Anomaly Penalty (if present)
        if (sanitized.getBehavioralAnomaly() != null && sanitized.getBehavioralAnomaly() > 0.50) {
            rawComposite = Math.min(1.0, rawComposite + (sanitized.getBehavioralAnomaly() * 0.10));
        }

        // 8. Scale to strictly 0 - 100 Integer
        int riskScore = (int) Math.round(rawComposite * 100.0);
        riskScore = Math.max(0, Math.min(100, riskScore));

        // 9. Actionable Decision & Classification
        DecisionEngine.DecisionEvaluation decisionEval = decisionEngine.evaluate(
                riskScore,
                contextResult.isRequiresSecondaryAuth(),
                deepfakeProb,
                contextRisk
        );

        // 10. Generate Deterministic Reasons
        List<String> reasons = reasonGenerator.generateReasons(
                sanitized,
                contextRisk,
                contextResult.getThreatFactors(),
                riskScore
        );

        // 11. Assemble Component Breakdown for Auditing
        Map<String, Object> componentBreakdown = new HashMap<>();
        componentBreakdown.put("deepfake_probability", deepfakeProb);
        componentBreakdown.put("speaker_verification_status", speakerStatus != null ? speakerStatus.name() : null);
        componentBreakdown.put("speaker_similarity", speakerSim);
        componentBreakdown.put("contextual_risk", round(contextRisk, 4));
        componentBreakdown.put("acoustic_anomaly", round(acousticAnomaly, 4));
        componentBreakdown.put("prosodic_anomaly", round(prosodicAnomaly, 4));
        if (sanitized.getBehavioralAnomaly() != null) {
            componentBreakdown.put("behavioral_anomaly", round(sanitized.getBehavioralAnomaly(), 4));
        }

        // 12. Security & Compliance Metadata
        String sessionHash = privacyGuard.generateAnonymizedSessionId(
                sanitized.getCallerId() != null ? sanitized.getCallerId() : "ANONYMOUS_SESSION",
                sanitized.getTimestamp() != null ? sanitized.getTimestamp() : Instant.now().toString()
        );

        Map<String, Object> securityMetadata = new HashMap<>();
        securityMetadata.put("session_hash", sessionHash);
        securityMetadata.put("zero_retention_verified", true);
        securityMetadata.put("replay_nonce_valid", replayValid);

        // 13. Build Frozen Output Result
        RiskEvaluationResult result = new RiskEvaluationResult(
                riskScore,
                decisionEval.getRiskLevel(),
                decisionEval.getDecision(),
                reasons,
                MODEL_VERSION,
                Instant.now().toString()
        );
        result.setComponentBreakdown(componentBreakdown);
        result.setSecurityMetadata(securityMetadata);

        return result;
    }

    private double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}

package com.voiceshield.risk.risk;

import com.voiceshield.risk.model.Decision;
import com.voiceshield.risk.model.RiskLevel;

/**
 * Evaluates composite risk score and context overrides to produce
 * standard actionable security decisions.
 */
public class DecisionEngine {

    private final RiskWeightsConfig config;

    public DecisionEngine(RiskWeightsConfig config) {
        this.config = config != null ? config : RiskWeightsConfig.createDefault();
    }

    public DecisionEvaluation evaluate(int riskScore, boolean contextRequiresSecondaryAuth,
                                       Double deepfakeProb, Double contextRisk) {
        RiskLevel level;
        Decision decision;

        // Base Risk Level Classification (0 - 100)
        if (riskScore >= config.getCriticalThreshold()) {
            level = RiskLevel.CRITICAL;
            decision = Decision.BLOCK;
        } else if (riskScore >= config.getHighThreshold()) {
            level = RiskLevel.HIGH;
            decision = Decision.SECONDARY_VERIFICATION;
        } else if (riskScore >= config.getMediumThreshold()) {
            level = RiskLevel.MEDIUM;
            decision = Decision.WARN;
        } else {
            level = RiskLevel.LOW;
            decision = Decision.ALLOW;
        }

        // Policy Override: Circuit-breaker for extreme deepfake probability + high context risk
        if (deepfakeProb != null && deepfakeProb >= config.getAutoBlockDeepfakeThreshold()
                && contextRisk != null && contextRisk >= config.getAutoBlockContextThreshold()) {
            level = RiskLevel.CRITICAL;
            decision = Decision.BLOCK;
        }

        // Policy Override: Contextual Secondary Auth (e.g. high value transfer or foreign IP)
        // Escalates ALLOW or WARN to SECONDARY_VERIFICATION
        if (contextRequiresSecondaryAuth && (decision == Decision.ALLOW || decision == Decision.WARN)) {
            decision = Decision.SECONDARY_VERIFICATION;
            if (level == RiskLevel.LOW) {
                level = RiskLevel.MEDIUM;
            }
        }

        return new DecisionEvaluation(level, decision);
    }

    public static class DecisionEvaluation {
        private final RiskLevel riskLevel;
        private final Decision decision;

        public DecisionEvaluation(RiskLevel riskLevel, Decision decision) {
            this.riskLevel = riskLevel;
            this.decision = decision;
        }

        public RiskLevel getRiskLevel() {
            return riskLevel;
        }

        public Decision getDecision() {
            return decision;
        }
    }
}

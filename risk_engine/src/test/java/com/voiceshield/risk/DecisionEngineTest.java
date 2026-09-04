package com.voiceshield.risk;

import com.voiceshield.risk.model.Decision;
import com.voiceshield.risk.model.RiskLevel;
import com.voiceshield.risk.risk.DecisionEngine;
import com.voiceshield.risk.risk.RiskWeightsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class DecisionEngineTest {

    private DecisionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DecisionEngine(RiskWeightsConfig.createDefault());
    }

    @ParameterizedTest
    @CsvSource({
            "0, LOW, ALLOW",
            "24, LOW, ALLOW",
            "25, MEDIUM, WARN",
            "49, MEDIUM, WARN",
            "50, HIGH, SECONDARY_VERIFICATION",
            "74, HIGH, SECONDARY_VERIFICATION",
            "75, CRITICAL, BLOCK",
            "100, CRITICAL, BLOCK"
    })
    @DisplayName("Should strictly classify risk levels and decisions across boundary thresholds")
    void testThresholdBoundaries(int score, RiskLevel expectedLevel, Decision expectedDecision) {
        DecisionEngine.DecisionEvaluation eval = engine.evaluate(score, false, 0.0, 0.0);

        assertThat(eval.getRiskLevel()).isEqualTo(expectedLevel);
        assertThat(eval.getDecision()).isEqualTo(expectedDecision);
    }

    @Test
    @DisplayName("Should auto-block on circuit breaker override (deepfake >= 0.90 and context >= 0.70)")
    void testCircuitBreakerOverride() {
        // Even if score was somehow low (e.g. 45), extreme deepfake + context override blocks transaction
        DecisionEngine.DecisionEvaluation eval = engine.evaluate(45, false, 0.95, 0.75);

        assertThat(eval.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(eval.getDecision()).isEqualTo(Decision.BLOCK);
    }

    @Test
    @DisplayName("Should escalate ALLOW to SECONDARY_VERIFICATION if context requires secondary auth")
    void testContextSecondaryAuthEscalation() {
        DecisionEngine.DecisionEvaluation eval = engine.evaluate(15, true, 0.10, 0.45);

        assertThat(eval.getDecision()).isEqualTo(Decision.SECONDARY_VERIFICATION);
        assertThat(eval.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }
}

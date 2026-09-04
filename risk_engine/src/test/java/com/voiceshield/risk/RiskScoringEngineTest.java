package com.voiceshield.risk;

import com.voiceshield.risk.mock.MockSignalFactory;
import com.voiceshield.risk.model.*;
import com.voiceshield.risk.risk.RiskScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RiskScoringEngineTest {

    private RiskScoringEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RiskScoringEngine();
    }

    @Test
    @DisplayName("Should evaluate the exact prompt attack scenario and trigger BLOCK with CRITICAL risk")
    void testPromptExampleAttack() {
        RiskSignalInput input = MockSignalFactory.createPromptExampleAttack();
        RiskEvaluationResult result = engine.evaluate(input);

        assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(75);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(result.getDecision()).isEqualTo(Decision.BLOCK);
        assertThat(result.getReasons()).anyMatch(r -> r.contains("AI voice cloning"));
        assertThat(result.getReasons()).anyMatch(r -> r.contains("Speaker biometric mismatch"));
        assertThat(result.getModelVersion()).isEqualTo(RiskScoringEngine.MODEL_VERSION);
    }

    @Test
    @DisplayName("Should evaluate low-risk genuine human voice and allow transaction")
    void testLowRiskHuman() {
        RiskSignalInput input = MockSignalFactory.createLowRiskGenuineHuman();
        RiskEvaluationResult result = engine.evaluate(input);

        assertThat(result.getRiskScore()).isLessThan(25);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.getDecision()).isEqualTo(Decision.ALLOW);
        assertThat(result.getReasons()).anyMatch(r -> r.contains("genuine human"));
    }

    @Test
    @DisplayName("Should evaluate medium-risk scenario and return WARN")
    void testMediumRiskWarning() {
        RiskSignalInput input = MockSignalFactory.createMediumRiskWarning();
        RiskEvaluationResult result = engine.evaluate(input);

        assertThat(result.getRiskScore()).isBetween(25, 49);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.getDecision()).isEqualTo(Decision.WARN);
    }

    @Test
    @DisplayName("Should evaluate high-risk scenario and trigger SECONDARY_VERIFICATION")
    void testHighRiskSecondaryAuth() {
        RiskSignalInput input = MockSignalFactory.createHighRiskSecondaryAuth();
        RiskEvaluationResult result = engine.evaluate(input);

        assertThat(result.getRiskScore()).isBetween(50, 74);
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.getDecision()).isEqualTo(Decision.SECONDARY_VERIFICATION);
    }

    @Test
    @DisplayName("Should detect and explain contradictory signals (Deepfake 0.89 BUT Speaker MATCH)")
    void testContradictorySignalsTargetedClone() {
        RiskSignalInput input = MockSignalFactory.createContradictorySignalsTargetedClone();
        RiskEvaluationResult result = engine.evaluate(input);

        // Targeted voice clone of an enrolled VIP account must trigger critical alerts
        assertThat(result.getRiskScore()).isGreaterThanOrEqualTo(50);
        assertThat(result.getReasons()).anyMatch(r -> r.contains("SECURITY ALERT: Speaker matches enrolled profile BUT"));
    }

    @Test
    @DisplayName("Should gracefully handle missing speaker verification signals without inventing fake numbers")
    void testMissingSpeakerVerificationSignals() {
        RiskSignalInput input = MockSignalFactory.createMissingSignalsUnenrolledCustomer();
        RiskEvaluationResult result = engine.evaluate(input);

        // Checks that speaker_similarity remains null in the breakdown
        assertThat(result.getComponentBreakdown().get("speaker_similarity")).isNull();
        assertThat(result.getComponentBreakdown().get("speaker_verification_status")).isEqualTo("UNAVAILABLE");
        assertThat(result.getReasons()).anyMatch(r -> r.contains("speaker identity verification unavailable"));

        // Score must still be calculated cleanly based on fallback normalized weights
        assertThat(result.getRiskScore()).isBetween(0, 100);
    }

    @Test
    @DisplayName("Should produce completely deterministic results for identical input payloads")
    void testDeterminism() {
        RiskSignalInput input1 = MockSignalFactory.createPromptExampleAttack();
        RiskSignalInput input2 = MockSignalFactory.createPromptExampleAttack();

        RiskEvaluationResult res1 = engine.evaluate(input1);
        RiskEvaluationResult res2 = engine.evaluate(input2);

        assertThat(res1.getRiskScore()).isEqualTo(res2.getRiskScore());
        assertThat(res1.getRiskLevel()).isEqualTo(res2.getRiskLevel());
        assertThat(res1.getDecision()).isEqualTo(res2.getDecision());
        assertThat(res1.getReasons()).containsExactlyElementsOf(res2.getReasons());
    }
}

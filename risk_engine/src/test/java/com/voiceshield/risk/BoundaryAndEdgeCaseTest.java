package com.voiceshield.risk;

import com.voiceshield.risk.model.ContextMetadata;
import com.voiceshield.risk.model.RiskEvaluationResult;
import com.voiceshield.risk.model.RiskSignalInput;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import com.voiceshield.risk.risk.RiskScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BoundaryAndEdgeCaseTest {

    private RiskScoringEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RiskScoringEngine();
    }

    @Test
    @DisplayName("Should handle NaN and Infinity in probability inputs gracefully by sanitizing to 0.0")
    void testNaNAndInfinityHandling() {
        RiskSignalInput input = new RiskSignalInput();
        input.setDeepfakeProbability(Double.NaN);
        input.setAcousticAnomaly(Double.POSITIVE_INFINITY);
        input.setProsodicAnomaly(Double.NEGATIVE_INFINITY);

        RiskEvaluationResult result = engine.evaluate(input);

        assertThat(result.getRiskScore()).isBetween(0, 100);
        assertThat(result.getComponentBreakdown().get("deepfake_probability")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should clamp negative and oversized probability values strictly to [0.0, 1.0]")
    void testClampingNegativeAndOversizedValues() {
        RiskSignalInput input = new RiskSignalInput();
        input.setDeepfakeProbability(-0.75); // Should clamp to 0.0
        input.setAcousticAnomaly(1.85);      // Should clamp to 1.0

        RiskEvaluationResult result = engine.evaluate(input);

        assertThat(result.getRiskScore()).isBetween(0, 100);
        assertThat(result.getComponentBreakdown().get("deepfake_probability")).isEqualTo(0.0);
        assertThat(result.getComponentBreakdown().get("acoustic_anomaly")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should neutralize CRLF log injection attempts in session and caller IDs")
    void testLogInjectionSanitization() {
        RiskSignalInput input = new RiskSignalInput();
        input.setSessionId("sess_123\r\nINJECTED_LOG_RECORD_MALICIOUS");
        input.setCallerId("+91-9876543210\r\nAdminRole=Granted");

        RiskEvaluationResult result = engine.evaluate(input);

        assertThat(result.getRiskScore()).isBetween(0, 100);
        // Security metadata session hash should calculate cleanly without exception
        assertThat(result.getSecurityMetadata().get("session_hash")).isNotNull();
    }

    @Test
    @DisplayName("Should evaluate completely empty input object without NullPointerException")
    void testCompletelyEmptyInput() {
        RiskSignalInput emptyInput = new RiskSignalInput();
        RiskEvaluationResult result = engine.evaluate(emptyInput);

        assertThat(result.getRiskScore()).isBetween(0, 100);
        assertThat(result.getDecision()).isNotNull();
        assertThat(result.getRiskLevel()).isNotNull();
        assertThat(result.getReasons()).isNotEmpty();
    }

    @Test
    @DisplayName("Should protect against financial transaction amount overflow attacks")
    void testTransactionOverflowAttack() {
        ContextMetadata ctx = new ContextMetadata();
        ctx.setTransactionAmount(1e15); // Unrealistic trillion amount

        RiskSignalInput input = new RiskSignalInput(0.5, SpeakerVerificationStatus.UNAVAILABLE, null, ctx);
        RiskEvaluationResult result = engine.evaluate(input);

        assertThat(result.getRiskScore()).isBetween(0, 100);
    }
}

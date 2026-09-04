package com.voiceshield.risk;

import com.voiceshield.risk.context.ContextAnalyzer;
import com.voiceshield.risk.context.ContextRulesConfig;
import com.voiceshield.risk.model.ContextAnalysisResult;
import com.voiceshield.risk.model.ContextMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextAnalyzerTest {

    private ContextAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new ContextAnalyzer(ContextRulesConfig.createDefault());
    }

    @Test
    @DisplayName("Should return zero contextual threat for empty or benign metadata")
    void testBenignContext() {
        ContextMetadata meta = new ContextMetadata();
        meta.setTransactionAmount(2000.0);
        meta.setCallerTrustScore(0.99);

        ContextAnalysisResult result = analyzer.analyze(meta);

        assertThat(result.getContextualRisk()).isEqualTo(0.0);
        assertThat(result.isRequiresSecondaryAuth()).isFalse();
        assertThat(result.getThreatFactors()).isEmpty();
    }

    @Test
    @DisplayName("Should flag high-value financial transactions above ₹5 Lakhs")
    void testHighValueTransaction() {
        ContextMetadata meta = new ContextMetadata();
        meta.setTransactionAmount(650000.0);

        ContextAnalysisResult result = analyzer.analyze(meta);

        assertThat(result.getContextualRisk()).isEqualTo(0.35);
        assertThat(result.getThreatFactors()).anyMatch(f -> f.contains("High-value fund transfer"));
    }

    @Test
    @DisplayName("Should accumulate multiple threat factors up to 1.0 ceiling")
    void testAccumulatedThreatFactors() {
        ContextMetadata meta = new ContextMetadata();
        meta.setTransactionAmount(700000.0); // +0.35
        meta.setUrgency(true);                // +0.25
        meta.setNewBeneficiary(true);        // +0.20
        meta.setInternationalCall(true);      // +0.15
        meta.setCallerTrustScore(0.20);      // +0.15 -> total = 1.10 -> clamped to 1.00

        ContextAnalysisResult result = analyzer.analyze(meta);

        assertThat(result.getContextualRisk()).isEqualTo(1.0);
        assertThat(result.isRequiresSecondaryAuth()).isTrue();
        assertThat(result.getThreatFactors()).hasSize(5);
    }

    @Test
    @DisplayName("Should trigger secondary authentication requirement when context risk exceeds threshold")
    void testSecondaryAuthTrigger() {
        ContextMetadata meta = new ContextMetadata();
        meta.setTransactionAmount(150000.0); // +0.20
        meta.setUrgency(true);                // +0.25 -> total 0.45 >= 0.40

        ContextAnalysisResult result = analyzer.analyze(meta);

        assertThat(result.getContextualRisk()).isGreaterThanOrEqualTo(0.40);
        assertThat(result.isRequiresSecondaryAuth()).isTrue();
    }

    @Test
    @DisplayName("Should handle null context metadata safely without NPE")
    void testNullContext() {
        ContextAnalysisResult result = analyzer.analyze(null);

        assertThat(result.getContextualRisk()).isEqualTo(0.0);
        assertThat(result.isRequiresSecondaryAuth()).isFalse();
        assertThat(result.getThreatFactors()).isEmpty();
    }
}

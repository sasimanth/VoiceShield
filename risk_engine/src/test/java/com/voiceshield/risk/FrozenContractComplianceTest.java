package com.voiceshield.risk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voiceshield.risk.mock.MockSignalFactory;
import com.voiceshield.risk.model.RiskEvaluationResult;
import com.voiceshield.risk.model.RiskSignalInput;
import com.voiceshield.risk.risk.RiskScoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class FrozenContractComplianceTest {

    private RiskScoringEngine engine;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        engine = new RiskScoringEngine();
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should strictly satisfy all required fields in integration/contracts/risk_contract.schema.json")
    void testContractSchemaConformance() throws Exception {
        RiskSignalInput input = MockSignalFactory.createPromptExampleAttack();
        RiskEvaluationResult result = engine.evaluate(input);

        String json = mapper.writeValueAsString(result);
        JsonNode root = mapper.readTree(json);

        // 1. Validate Required Fields
        assertThat(root.hasNonNull("risk_score")).isTrue();
        assertThat(root.hasNonNull("risk_level")).isTrue();
        assertThat(root.hasNonNull("decision")).isTrue();
        assertThat(root.hasNonNull("reasons")).isTrue();
        assertThat(root.hasNonNull("model_version")).isTrue();
        assertThat(root.hasNonNull("timestamp")).isTrue();

        // 2. Validate Types and Bounds
        assertThat(root.get("risk_score").isInt()).isTrue();
        int score = root.get("risk_score").asInt();
        assertThat(score).isBetween(0, 100);

        // Crucial SIH Requirement: Do not silently change 0-100 to 0-1 or vice versa!
        assertThat(root.get("risk_score").isFloatingPointNumber()).isFalse();

        // 3. Validate Enum Ranges
        String riskLevel = root.get("risk_level").asText();
        assertThat(riskLevel).isIn("LOW", "MEDIUM", "HIGH", "CRITICAL");

        String decision = root.get("decision").asText();
        assertThat(decision).isIn("ALLOW", "WARN", "SECONDARY_VERIFICATION", "BLOCK");

        // 4. Validate Reasons Array
        assertThat(root.get("reasons").isArray()).isTrue();
        assertThat(root.get("reasons").size()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should match frozen contract sample integration/contracts/risk_contract.json structure")
    void testMatchesFrozenSampleContract() throws Exception {
        Path contractPath = Path.of("../integration/contracts/risk_contract.json");
        if (!Files.exists(contractPath)) {
            contractPath = Path.of("integration/contracts/risk_contract.json");
        }

        if (Files.exists(contractPath)) {
            String sampleJson = Files.readString(contractPath);
            JsonNode sampleNode = mapper.readTree(sampleJson);

            // Verify the sample has identical keys to our produced model
            RiskSignalInput input = MockSignalFactory.createPromptExampleAttack();
            RiskEvaluationResult result = engine.evaluate(input);
            JsonNode generatedNode = mapper.readTree(mapper.writeValueAsString(result));

            sampleNode.fieldNames().forEachRemaining(field -> {
                assertThat(generatedNode.has(field))
                        .as("Generated contract must contain key: " + field)
                        .isTrue();
            });
        }
    }
}

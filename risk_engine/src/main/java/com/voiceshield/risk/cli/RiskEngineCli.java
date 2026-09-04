package com.voiceshield.risk.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voiceshield.risk.mock.MockSignalFactory;
import com.voiceshield.risk.model.RiskEvaluationResult;
import com.voiceshield.risk.model.RiskSignalInput;
import com.voiceshield.risk.risk.RiskScoringEngine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Command-Line Interface and Interoperability Bridge for Person 3 & Person 6.
 * Evaluates inputs from CLI arguments, files, stdin, or pre-canned mocks,
 * and outputs the frozen JSON contract to stdout.
 */
public class RiskEngineCli {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        try {
            RiskScoringEngine engine = new RiskScoringEngine();
            RiskSignalInput input;

            if (args.length >= 2 && "--mock".equalsIgnoreCase(args[0])) {
                String scenario = args[1].toLowerCase();
                input = switch (scenario) {
                    case "attack", "critical" -> MockSignalFactory.createPromptExampleAttack();
                    case "low", "human" -> MockSignalFactory.createLowRiskGenuineHuman();
                    case "medium", "warn" -> MockSignalFactory.createMediumRiskWarning();
                    case "high", "secondary" -> MockSignalFactory.createHighRiskSecondaryAuth();
                    case "contradictory" -> MockSignalFactory.createContradictorySignalsTargetedClone();
                    case "missing", "unenrolled" -> MockSignalFactory.createMissingSignalsUnenrolledCustomer();
                    default -> throw new IllegalArgumentException("Unknown mock scenario: " + scenario);
                };
            } else if (args.length >= 2 && "--file".equalsIgnoreCase(args[0])) {
                String jsonContent = Files.readString(Path.of(args[1]));
                input = MAPPER.readValue(jsonContent, RiskSignalInput.class);
            } else if (args.length >= 1 && "--stdin".equalsIgnoreCase(args[0])) {
                String stdinJson = new BufferedReader(new InputStreamReader(System.in))
                        .lines().collect(Collectors.joining("\n"));
                input = MAPPER.readValue(stdinJson, RiskSignalInput.class);
            } else {
                // Default: Run prompt example attack mock
                input = MockSignalFactory.createPromptExampleAttack();
            }

            RiskEvaluationResult result = engine.evaluate(input);
            String jsonOutput = MAPPER.writeValueAsString(result);
            System.out.println(jsonOutput);

        } catch (Exception e) {
            System.err.println("{\"error\": \"" + e.getMessage() + "\"}");
            System.exit(1);
        }
    }
}

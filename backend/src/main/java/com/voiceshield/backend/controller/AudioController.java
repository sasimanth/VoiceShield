package com.voiceshield.backend.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.voiceshield.backend.dto.AudioAnalysisResponse;
import com.voiceshield.backend.service.MlInferenceClient;
import com.voiceshield.backend.service.RiskEvaluationService;
import com.voiceshield.risk.model.ContextMetadata;
import com.voiceshield.risk.model.RiskEvaluationResult;
import com.voiceshield.risk.model.SpeakerVerificationStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/audio")
@Tag(
        name = "Audio Analysis",
        description = "Audio upload and AI voice integrity analysis"
)
public class AudioController {

    private final MlInferenceClient mlInferenceClient;
    private final RiskEvaluationService riskEvaluationService;

    public AudioController(
            MlInferenceClient mlInferenceClient,
            RiskEvaluationService riskEvaluationService
    ) {
        this.mlInferenceClient = mlInferenceClient;
        this.riskEvaluationService = riskEvaluationService;
    }

    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Analyze Audio",
            description = "Analyzes an uploaded audio file for synthetic/deepfake voice risk"
    )
    public ResponseEntity<AudioAnalysisResponse> analyzeAudio(
            @RequestPart("file") MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }

        try {

            long startTime = System.currentTimeMillis();

            byte[] audioBytes = file.getBytes();

            String sessionId =
                    "sess-" + UUID.randomUUID()
                            .toString()
                            .substring(0, 8);

            /*
             * Step 1:
             * Send audio to ML service.
             *
             * If the Python ML service is unavailable,
             * MlInferenceClient currently provides fallback
             * development features.
             */
            Map<String, Object> mlFeatures =
                    mlInferenceClient.analyzeAudio(
                            audioBytes,
                            file.getOriginalFilename()
                    );

            double deepfakeProbability =
                    getDouble(
                            mlFeatures,
                            "deepfake_probability",
                            0.05
                    );

            double acousticAnomaly =
                    getDouble(
                            mlFeatures,
                            "acoustic_anomaly",
                            0.10
                    );

            double prosodicAnomaly =
                    getDouble(
                            mlFeatures,
                            "prosodic_anomaly",
                            0.10
                    );

            double behavioralAnomaly =
                    getDouble(
                            mlFeatures,
                            "behavioral_anomaly",
                            0.10
                    );

            /*
             * Speaker verification is not connected yet.
             *
             * Therefore we explicitly mark it UNAVAILABLE
             * instead of pretending that verification happened.
             */
            RiskEvaluationResult riskResult =
                    riskEvaluationService.evaluateFromSignals(
                            null,
                            deepfakeProbability,
                            SpeakerVerificationStatus.UNAVAILABLE,
                            null,
                            acousticAnomaly,
                            prosodicAnomaly,
                            behavioralAnomaly,
                            new ContextMetadata()
                    );

            long processingTime =
                    System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(
                    new AudioAnalysisResponse(
                            riskResult,
                            sessionId,
                            mlFeatures
                    )
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Audio analysis failed: " + e.getMessage()
            );
        }
    }

    private double getDouble(
            Map<String, Object> data,
            String key,
            double defaultValue
    ) {

        Object value = data.get(key);

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return defaultValue;
    }
}
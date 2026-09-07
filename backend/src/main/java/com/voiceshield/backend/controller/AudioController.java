package com.voiceshield.backend.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.voiceshield.backend.client.MlInferenceClient;
import com.voiceshield.backend.dto.AudioAnalysisResponse;
import com.voiceshield.backend.dto.MlInferenceResponse;
import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.backend.service.RiskEvaluationService;
import com.voiceshield.backend.service.SpeakerVerificationService;
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
    private final SpeakerVerificationService speakerVerificationService;

    public AudioController(
            MlInferenceClient mlInferenceClient,
            RiskEvaluationService riskEvaluationService,
            SpeakerVerificationService speakerVerificationService
    ) {
        this.mlInferenceClient = mlInferenceClient;
        this.riskEvaluationService = riskEvaluationService;
        this.speakerVerificationService = speakerVerificationService;
    }

    @PostMapping(
            value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Analyze Audio",
            description = "Analyzes an uploaded audio file for synthetic/deepfake voice risk and optional speaker verification"
    )
    public ResponseEntity<AudioAnalysisResponse> analyzeAudio(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "speaker_id", required = false) String speakerId
    ) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }

        try {

            byte[] audioBytes = file.getBytes();

            String sessionId = "sess-" + UUID.randomUUID().toString().substring(0, 8);

            // 1. Dispatch Voice Integrity / Deepfake Analysis to Python (AASIST)
            MlInferenceResponse mlResponse = mlInferenceClient.analyzeAudio(
                    audioBytes,
                    file.getOriginalFilename()
            );

            // 2. Dispatch Speaker Verification if speaker_id is provided
            SpeakerVerificationStatus spkStatus = SpeakerVerificationStatus.UNAVAILABLE;
            Double spkSimilarity = null;

            if (speakerId != null && !speakerId.trim().isEmpty()) {
                SpeakerVerifyResponse verifyRes = speakerVerificationService.verifySpeaker(speakerId.trim(), audioBytes);
                if (verifyRes != null) {
                    spkStatus = verifyRes.getStatus();
                    spkSimilarity = verifyRes.getSimilarityScore();
                }
            }

            // 3. Evaluate Multi-Signal Composite Risk Score
            RiskEvaluationResult riskResult = riskEvaluationService.evaluateFromSignals(
                    speakerId,
                    mlResponse.getDeepfakeScore(),
                    spkStatus,
                    spkSimilarity,
                    mlResponse.getAcousticAnomaly(),
                    mlResponse.getProsodicAnomaly(),
                    mlResponse.getBehavioralAnomaly(),
                    new ContextMetadata()
            );

            Map<String, Object> rawMlFeatures = new HashMap<>();
            rawMlFeatures.put("deepfake_score", mlResponse.getDeepfakeScore());
            rawMlFeatures.put("acoustic_anomaly", mlResponse.getAcousticAnomaly());
            rawMlFeatures.put("prosodic_anomaly", mlResponse.getProsodicAnomaly());
            rawMlFeatures.put("behavioral_anomaly", mlResponse.getBehavioralAnomaly());
            rawMlFeatures.put("analysis_status", mlResponse.getAnalysisStatus());
            rawMlFeatures.put("ml_service_status", mlResponse.getMlServiceStatus());
            rawMlFeatures.put("speaker_verification_status", spkStatus.name());
            if (spkSimilarity != null) {
                rawMlFeatures.put("speaker_similarity", spkSimilarity);
            }

            return ResponseEntity.ok(
                    new AudioAnalysisResponse(
                            riskResult,
                            sessionId,
                            rawMlFeatures
                    )
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Audio analysis failed: " + e.getMessage()
            );
        }
    }
}
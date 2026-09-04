package com.voiceshield.backend.controller;

import com.voiceshield.backend.dto.AudioAnalysisResponse;
import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.backend.service.MlInferenceClient;
import com.voiceshield.backend.service.RiskEvaluationService;
import com.voiceshield.backend.service.SpeakerVerificationService;
import com.voiceshield.risk.model.ContextMetadata;
import com.voiceshield.risk.model.RiskEvaluationResult;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audio")
@Tag(name = "Audio Analysis", description = "End-to-end voice deepfake detection & contextual risk scoring")
public class AudioAnalysisController {

    private final MlInferenceClient mlInferenceClient;
    private final SpeakerVerificationService speakerService;
    private final RiskEvaluationService riskService;

    public AudioAnalysisController(
            MlInferenceClient mlInferenceClient,
            SpeakerVerificationService speakerService,
            RiskEvaluationService riskService
    ) {
        this.mlInferenceClient = mlInferenceClient;
        this.speakerService = speakerService;
        this.riskService = riskService;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze Voice Recording", description = "Ingests raw audio file, runs ML deepfake analysis, verifies speaker biometrics, and calculates explainable risk score")
    public ResponseEntity<AudioAnalysisResponse> analyzeVoice(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "claimed_speaker_id", required = false) String claimedSpeakerId,
            @RequestParam(value = "caller_id", required = false, defaultValue = "+91-9876543210") String callerId,
            @RequestParam(value = "transaction_amount", required = false) Double transactionAmount,
            @RequestParam(value = "currency", required = false, defaultValue = "INR") String currency,
            @RequestParam(value = "is_new_beneficiary", required = false, defaultValue = "false") boolean isNewBeneficiary,
            @RequestParam(value = "is_international_call", required = false, defaultValue = "false") boolean isInternationalCall,
            @RequestParam(value = "urgency", required = false, defaultValue = "false") boolean urgency,
            @RequestParam(value = "caller_trust_score", required = false, defaultValue = "0.85") double callerTrustScore,
            @RequestParam(value = "is_privileged_account", required = false, defaultValue = "false") boolean isPrivilegedAccount,
            @RequestParam(value = "failed_auth_attempts", required = false, defaultValue = "0") int failedAuthAttempts
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] audioBytes = file.getBytes();

            // 1. Layer 1 & 2: ML Deepfake & Acoustic/Prosodic Inference
            Map<String, Object> mlFeatures = mlInferenceClient.analyzeAudio(audioBytes, file.getOriginalFilename());
            Double deepfakeProb = getDoubleValue(mlFeatures.get("deepfake_probability"), 0.08);
            Double acousticAnomaly = getDoubleValue(mlFeatures.get("acoustic_anomaly"), 0.10);
            Double prosodicAnomaly = getDoubleValue(mlFeatures.get("prosodic_anomaly"), 0.12);
            Double behavioralAnomaly = getDoubleValue(mlFeatures.get("behavioral_anomaly"), 0.10);

            // 2. Layer 3: Speaker Biometric Verification
            SpeakerVerificationStatus speakerStatus = SpeakerVerificationStatus.UNAVAILABLE;
            Double speakerSimilarity = null;

            if (claimedSpeakerId != null && !claimedSpeakerId.trim().isEmpty()) {
                SpeakerVerifyResponse speakerRes = speakerService.verifySpeaker(claimedSpeakerId, audioBytes);
                speakerStatus = speakerRes.getStatus();
                speakerSimilarity = speakerRes.getSimilarityScore();
            }

            // 3. Layer 4: Contextual Threat Modeling
            ContextMetadata contextMetadata = new ContextMetadata();
            contextMetadata.setTransactionAmount(transactionAmount);
            contextMetadata.setCurrency(currency);
            contextMetadata.setNewBeneficiary(isNewBeneficiary);
            contextMetadata.setInternationalCall(isInternationalCall);
            contextMetadata.setUrgency(urgency);
            contextMetadata.setCallerTrustScore(callerTrustScore);
            contextMetadata.setPrivilegedAccount(isPrivilegedAccount);
            contextMetadata.setFailedAuthAttempts(failedAuthAttempts);

            // 4. Layer 5: Risk Scoring Engine Execution
            RiskEvaluationResult riskResult = riskService.evaluateFromSignals(
                    callerId,
                    deepfakeProb,
                    speakerStatus,
                    speakerSimilarity,
                    acousticAnomaly,
                    prosodicAnomaly,
                    behavioralAnomaly,
                    contextMetadata
            );

            String sessionId = "sess-" + UUID.randomUUID().toString().substring(0, 8);
            return ResponseEntity.ok(new AudioAnalysisResponse(riskResult, sessionId, mlFeatures));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/sample")
    @Operation(summary = "Sample Telemetry", description = "Returns sample contextual structure for client testing")
    public ResponseEntity<Map<String, Object>> getSample() {
        return ResponseEntity.ok(Map.of(
                "caller_id", "+91-9876543210",
                "claimed_speaker_id", "CEO-EXECUTIVE-101",
                "transaction_amount", 500000.00,
                "currency", "INR",
                "urgency", true,
                "is_new_beneficiary", true,
                "is_international_call", false
        ));
    }

    private Double getDoubleValue(Object obj, Double defaultVal) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return defaultVal;
    }
}

package com.voiceshield.backend.service;

import com.voiceshield.risk.model.ContextMetadata;
import com.voiceshield.risk.model.RiskEvaluationResult;
import com.voiceshield.risk.model.RiskSignalInput;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import com.voiceshield.risk.risk.RiskScoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RiskEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(RiskEvaluationService.class);
    private final RiskScoringEngine riskScoringEngine;

    public RiskEvaluationService() {
        this.riskScoringEngine = new RiskScoringEngine();
        log.info("Initialized VoiceShield RiskScoringEngine (v1.2.0-composite-risk) within Spring Boot");
    }

    public RiskEvaluationResult evaluate(RiskSignalInput input) {
        log.debug("Evaluating RiskSignalInput for caller: {}", input.getCallerId());
        return riskScoringEngine.evaluate(input);
    }

    public RiskEvaluationResult evaluateFromSignals(
            String callerId,
            Double deepfakeProbability,
            SpeakerVerificationStatus speakerStatus,
            Double speakerSimilarity,
            Double acousticAnomaly,
            Double prosodicAnomaly,
            Double behavioralAnomaly,
            ContextMetadata context
    ) {
        String sessionId = "sess-" + UUID.randomUUID().toString().substring(0, 8);
        String nonce = UUID.randomUUID().toString();

        RiskSignalInput input = new RiskSignalInput();
        input.setSessionId(sessionId);
        input.setCallerId(callerId != null ? callerId : "+91-9876543210");
        input.setTimestamp(Instant.now().toString());
        input.setNonce(nonce);
        input.setDeepfakeProbability(deepfakeProbability != null ? deepfakeProbability : 0.05);
        input.setSpeakerVerificationStatus(speakerStatus != null ? speakerStatus : SpeakerVerificationStatus.UNAVAILABLE);
        input.setSpeakerSimilarity(speakerSimilarity);
        input.setAcousticAnomaly(acousticAnomaly != null ? acousticAnomaly : 0.1);
        input.setProsodicAnomaly(prosodicAnomaly != null ? prosodicAnomaly : 0.1);
        input.setBehavioralAnomaly(behavioralAnomaly != null ? behavioralAnomaly : 0.1);
        input.setContext(context != null ? context : new ContextMetadata());

        return riskScoringEngine.evaluate(input);
    }
}

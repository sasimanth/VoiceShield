package com.voiceshield.backend.service;

import com.voiceshield.backend.client.MlInferenceClient;
import com.voiceshield.backend.dto.MlSpeakerEmbeddingResponse;
import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.backend.util.VectorUtils;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SpeakerVerificationService {

    private static final Logger log = LoggerFactory.getLogger(SpeakerVerificationService.class);

    private final SpeakerProfileService speakerProfileService;
    private final MlInferenceClient mlInferenceClient;
    private final Double threshold;

    @Autowired
    public SpeakerVerificationService(
            SpeakerProfileService speakerProfileService,
            MlInferenceClient mlInferenceClient,
            @Value("${voiceshield.speaker-verification.threshold:#{null}}") Double threshold) {
        this.speakerProfileService = speakerProfileService;
        this.mlInferenceClient = mlInferenceClient;
        this.threshold = threshold;
        if (this.threshold != null) {
            log.info("Initialized SpeakerVerificationService with active production threshold θ_prod = {}", this.threshold);
        } else {
            log.warn("Initialized SpeakerVerificationService with UNCONFIGURED threshold (status = UNAVAILABLE)");
        }
    }

    public SpeakerVerificationService(
            SpeakerProfileService speakerProfileService,
            MlInferenceClient mlInferenceClient) {
        this(speakerProfileService, mlInferenceClient, 0.4000);
    }

    public Double getThreshold() {
        return threshold;
    }

    public boolean enrollSpeaker(String speakerId, byte[] audioBytes) {
        if (speakerId == null || speakerId.trim().isEmpty() || audioBytes == null || audioBytes.length == 0) {
            log.warn("Invalid speakerId or empty audioBytes passed to enrollSpeaker");
            return false;
        }

        MlSpeakerEmbeddingResponse pyResponse = mlInferenceClient.extractSpeakerEmbedding(audioBytes, "enroll.wav");
        if (pyResponse == null || pyResponse.getEmbedding() == null || pyResponse.getEmbedding().size() != VectorUtils.EXPECTED_DIMENSION) {
            log.warn("ECAPA embedding extraction failed for speaker '{}'. Enrollment failed.", speakerId);
            return false;
        }

        float[] embedding = VectorUtils.doubleListToFloatArray(pyResponse.getEmbedding());
        if (embedding == null) {
            log.error("Failed to parse embedding vector for speaker '{}'. Enrollment failed.", speakerId);
            return false;
        }

        speakerProfileService.saveOrUpdateEmbeddingProfile(speakerId, embedding);
        log.info("Speaker '{}' enrolled successfully with 192-D ECAPA embedding.", speakerId);
        return true;
    }

    public SpeakerVerifyResponse verifySpeaker(String speakerId, byte[] audioBytes) {
        if (speakerId == null || speakerId.trim().isEmpty() || audioBytes == null || audioBytes.length == 0) {
            log.warn("Invalid speakerId or empty audioBytes passed to verifySpeaker");
            return new SpeakerVerifyResponse(speakerId, SpeakerVerificationStatus.UNAVAILABLE, null, false, null);
        }

        // 1. Retrieve enrolled reference embedding vector from PostgreSQL DB
        float[] refEmbeddingFloat = speakerProfileService.getSpeakerEmbedding(speakerId);
        if (refEmbeddingFloat == null) {
            log.info("No enrolled speaker profile found in DB for speaker ID '{}'", speakerId);
            return new SpeakerVerifyResponse(speakerId, SpeakerVerificationStatus.UNAVAILABLE, null, false, null);
        }

        // 2. Extract query embedding vector from Python ML service (POST /speaker/embed)
        MlSpeakerEmbeddingResponse pyResponse = mlInferenceClient.extractSpeakerEmbedding(audioBytes, "verify.wav");
        if (pyResponse == null || pyResponse.getEmbedding() == null || pyResponse.getEmbedding().size() != VectorUtils.EXPECTED_DIMENSION) {
            log.warn("ML embedding service unavailable for verification of speaker '{}'. Returning UNAVAILABLE status.", speakerId);
            return new SpeakerVerifyResponse(speakerId, SpeakerVerificationStatus.UNAVAILABLE, null, false, null);
        }

        float[] queryEmbeddingFloat = VectorUtils.doubleListToFloatArray(pyResponse.getEmbedding());
        if (queryEmbeddingFloat == null) {
            return new SpeakerVerifyResponse(speakerId, SpeakerVerificationStatus.UNAVAILABLE, null, false, null);
        }

        // 3. Compute Java-side Cosine Similarity in double precision
        double[] refVec = VectorUtils.floatArrayToDoubleArray(refEmbeddingFloat);
        double[] queryVec = VectorUtils.floatArrayToDoubleArray(queryEmbeddingFloat);

        try {
            double rawCosineSimilarity = VectorUtils.cosineSimilarity(refVec, queryVec);

            if (this.threshold == null) {
                log.warn("Speaker verification threshold is unconfigured for speaker '{}'. Returning UNAVAILABLE status.", speakerId);
                return new SpeakerVerifyResponse(
                        speakerId,
                        SpeakerVerificationStatus.UNAVAILABLE,
                        rawCosineSimilarity,
                        false,
                        null
                );
            }

            boolean isMatch = rawCosineSimilarity >= this.threshold;
            SpeakerVerificationStatus status = isMatch ? SpeakerVerificationStatus.MATCH : SpeakerVerificationStatus.MISMATCH;

            log.info("Speaker verification decision for '{}': rawSimilarity={}, threshold={}, status={}, isMatch={}",
                    speakerId, rawCosineSimilarity, this.threshold, status, isMatch);

            return new SpeakerVerifyResponse(
                    speakerId,
                    status,
                    rawCosineSimilarity,
                    isMatch,
                    this.threshold
            );

        } catch (IllegalArgumentException e) {
            log.warn("Vector similarity calculation invalid for speaker '{}': {}. Returning UNAVAILABLE.", speakerId, e.getMessage());
            return new SpeakerVerifyResponse(speakerId, SpeakerVerificationStatus.UNAVAILABLE, null, false, null);
        } catch (Exception e) {
            log.error("Failed to compute Cosine Similarity for speaker '{}': {}", speakerId, e.getMessage());
            return new SpeakerVerifyResponse(speakerId, SpeakerVerificationStatus.UNAVAILABLE, null, false, null);
        }
    }
}
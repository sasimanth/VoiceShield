package com.voiceshield.backend.service;

import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.backend.entity.SpeakerProfile;
import com.voiceshield.backend.repository.SpeakerProfileRepository;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

@Service
public class SpeakerVerificationService {

    private static final Logger log =
            LoggerFactory.getLogger(SpeakerVerificationService.class);

    private static final int EMBEDDING_DIMENSION = 192;
    private static final int FLOAT32_BYTES = 4;
    private static final int EMBEDDING_BYTE_LENGTH =
            EMBEDDING_DIMENSION * FLOAT32_BYTES;

    private static final double MIN_NORM = 1e-12;

    private final SpeakerProfileRepository speakerProfileRepository;
    private final MlInferenceClient mlInferenceClient;

    @Value("${voiceshield.speaker-verification.similarity-threshold}")
    private double similarityThreshold;

    public SpeakerVerificationService(
            SpeakerProfileRepository speakerProfileRepository,
            MlInferenceClient mlInferenceClient) {

        this.speakerProfileRepository = speakerProfileRepository;
        this.mlInferenceClient = mlInferenceClient;
    }

    @Transactional
    public boolean enrollSpeaker(String speakerId, byte[] audioBytes) {

        if (speakerId == null || speakerId.trim().isEmpty()) {
            log.warn("Speaker enrollment rejected: speaker ID is empty");
            return false;
        }

        if (audioBytes == null || audioBytes.length == 0) {
            log.warn("Speaker enrollment rejected: audio is empty");
            return false;
        }

        String normalizedSpeakerId = speakerId.trim();

        try {
            double[] embedding =
                    mlInferenceClient.generateSpeakerEmbedding(
                            audioBytes,
                            normalizedSpeakerId + ".wav"
                    );

            validateEmbedding(embedding);

            byte[] embeddingBytes = embeddingToBytes(embedding);

            SpeakerProfile profile =
                    speakerProfileRepository
                            .findBySpeakerId(normalizedSpeakerId)
                            .orElseGet(SpeakerProfile::new);

            profile.setSpeakerId(normalizedSpeakerId);
            profile.setAudioData(embeddingBytes);
            profile.setEmbeddingDimension(EMBEDDING_DIMENSION);

            speakerProfileRepository.save(profile);

            log.info(
                    "Successfully enrolled ECAPA speaker profile for speakerId: {}",
                    normalizedSpeakerId
            );

            return true;

        } catch (Exception e) {
            log.error(
                    "Speaker enrollment failed for speakerId {}: {}",
                    normalizedSpeakerId,
                    e.getMessage()
            );

            return false;
        }
    }

    @Transactional(readOnly = true)
    public SpeakerVerifyResponse verifySpeaker(
            String claimedSpeakerId,
            byte[] audioBytes) {

        if (claimedSpeakerId == null
                || claimedSpeakerId.trim().isEmpty()) {

            return unavailableResponse(
                    claimedSpeakerId,
                    "Speaker ID is missing"
            );
        }

        if (audioBytes == null || audioBytes.length == 0) {

            return unavailableResponse(
                    claimedSpeakerId,
                    "Audio is empty"
            );
        }

        String normalizedSpeakerId = claimedSpeakerId.trim();

        Optional<SpeakerProfile> profileOptional =
                speakerProfileRepository.findBySpeakerId(
                        normalizedSpeakerId
                );

        if (profileOptional.isEmpty()) {

            log.warn(
                    "Speaker ID '{}' not enrolled in biometric database",
                    normalizedSpeakerId
            );

            return unavailableResponse(
                    normalizedSpeakerId,
                    "Speaker is not enrolled"
            );
        }

        try {
            double[] probeEmbedding =
                    mlInferenceClient.generateSpeakerEmbedding(
                            audioBytes,
                            normalizedSpeakerId + "-probe.wav"
                    );

            validateEmbedding(probeEmbedding);

            byte[] storedBytes =
                    profileOptional.get().getAudioData();

            double[] referenceEmbedding =
                    bytesToEmbedding(storedBytes);

            validateEmbedding(referenceEmbedding);

            double similarity =
                    cosineSimilarity(
                            referenceEmbedding,
                            probeEmbedding
                    );

            boolean isMatch =
                    similarity >= similarityThreshold;

            SpeakerVerificationStatus status =
                    isMatch
                            ? SpeakerVerificationStatus.MATCH
                            : SpeakerVerificationStatus.MISMATCH;

            return new SpeakerVerifyResponse(
                    normalizedSpeakerId,
                    status,
                    similarity,
                    isMatch,
                    similarityThreshold
            );

        } catch (IllegalArgumentException e) {

            log.warn(
                    "Invalid speaker embedding for speakerId {}: {}",
                    normalizedSpeakerId,
                    e.getMessage()
            );

            return unavailableResponse(
                    normalizedSpeakerId,
                    "Invalid speaker embedding"
            );

        } catch (Exception e) {

            log.error(
                    "Speaker verification failed for speakerId {}: {}",
                    normalizedSpeakerId,
                    e.getMessage()
            );

            return unavailableResponse(
                    normalizedSpeakerId,
                    "Speaker verification unavailable"
            );
        }
    }

    private SpeakerVerifyResponse unavailableResponse(
            String speakerId,
            String reason) {

        log.warn(
                "Speaker verification unavailable for '{}': {}",
                speakerId,
                reason
        );

        return new SpeakerVerifyResponse(
                speakerId,
                SpeakerVerificationStatus.UNAVAILABLE,
                null,
                false,
                similarityThreshold
        );
    }

    private static void validateEmbedding(double[] embedding) {

        if (embedding == null) {
            throw new IllegalArgumentException(
                    "Embedding must not be null"
            );
        }

        if (embedding.length != EMBEDDING_DIMENSION) {
            throw new IllegalArgumentException(
                    "Embedding must contain exactly 192 dimensions"
            );
        }

        double normSquared = 0.0;

        for (double value : embedding) {

            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException(
                        "Embedding contains NaN or Infinity"
                );
            }

            normSquared += value * value;
        }

        double norm = Math.sqrt(normSquared);

        if (!Double.isFinite(norm) || norm <= MIN_NORM) {
            throw new IllegalArgumentException(
                    "Embedding has zero or near-zero norm"
            );
        }
    }

    private static double cosineSimilarity(
            double[] first,
            double[] second) {

        validateEmbedding(first);
        validateEmbedding(second);

        if (first.length != second.length) {
            throw new IllegalArgumentException(
                    "Embedding dimensions do not match"
            );
        }

        double dotProduct = 0.0;
        double firstNormSquared = 0.0;
        double secondNormSquared = 0.0;

        for (int i = 0; i < first.length; i++) {

            dotProduct += first[i] * second[i];
            firstNormSquared += first[i] * first[i];
            secondNormSquared += second[i] * second[i];
        }

        double firstNorm = Math.sqrt(firstNormSquared);
        double secondNorm = Math.sqrt(secondNormSquared);

        if (!Double.isFinite(firstNorm)
                || !Double.isFinite(secondNorm)
                || firstNorm <= MIN_NORM
                || secondNorm <= MIN_NORM) {

            throw new IllegalArgumentException(
                    "Cannot calculate cosine similarity for zero/near-zero vector"
            );
        }

        double similarity =
                dotProduct / (firstNorm * secondNorm);

        if (!Double.isFinite(similarity)) {
            throw new IllegalArgumentException(
                    "Cosine similarity is not finite"
            );
        }

        return similarity;
    }

    private static byte[] embeddingToBytes(double[] embedding) {

        validateEmbedding(embedding);

        ByteBuffer buffer =
                ByteBuffer
                        .allocate(EMBEDDING_BYTE_LENGTH)
                        .order(ByteOrder.LITTLE_ENDIAN);

        for (double value : embedding) {
            buffer.putFloat((float) value);
        }

        return buffer.array();
    }

    private static double[] bytesToEmbedding(byte[] bytes) {

        if (bytes == null) {
            throw new IllegalArgumentException(
                    "Stored embedding must not be null"
            );
        }

        if (bytes.length != EMBEDDING_BYTE_LENGTH) {
            throw new IllegalArgumentException(
                    "Stored embedding must contain exactly 768 bytes"
            );
        }

        ByteBuffer buffer =
                ByteBuffer
                        .wrap(bytes)
                        .order(ByteOrder.LITTLE_ENDIAN);

        double[] embedding =
                new double[EMBEDDING_DIMENSION];

        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding[i] = buffer.getFloat();
        }

        return embedding;
    }
}
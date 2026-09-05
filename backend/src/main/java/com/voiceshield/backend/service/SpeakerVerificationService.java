package com.voiceshield.backend.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.risk.model.SpeakerVerificationStatus;

@Service
public class SpeakerVerificationService {

    private static final double DEFAULT_THRESHOLD = 0.75;

    /*
     * Temporary development storage.
     *
     * Later this will be replaced by the actual
     * speaker-verification service from Person 2.
     */
    private final Map<String, byte[]> enrolledSpeakers =
            new ConcurrentHashMap<>();

    public boolean enrollSpeaker(
            String speakerId,
            byte[] audioBytes
    ) {

        if (speakerId == null ||
                speakerId.isBlank()) {

            return false;
        }

        if (audioBytes == null ||
                audioBytes.length == 0) {

            return false;
        }

        enrolledSpeakers.put(
                speakerId,
                audioBytes
        );

        return true;
    }

    public SpeakerVerifyResponse verifySpeaker(
            String speakerId,
            byte[] audioBytes
    ) {

        if (speakerId == null ||
                speakerId.isBlank()) {

            return new SpeakerVerifyResponse(
                    speakerId,
                    SpeakerVerificationStatus.UNAVAILABLE,
                    null,
                    false,
                    DEFAULT_THRESHOLD
            );
        }

        if (audioBytes == null ||
                audioBytes.length == 0) {

            return new SpeakerVerifyResponse(
                    speakerId,
                    SpeakerVerificationStatus.UNAVAILABLE,
                    null,
                    false,
                    DEFAULT_THRESHOLD
            );
        }

        byte[] enrolledAudio =
                enrolledSpeakers.get(speakerId);

        if (enrolledAudio == null) {

            return new SpeakerVerifyResponse(
                    speakerId,
                    SpeakerVerificationStatus.UNAVAILABLE,
                    null,
                    false,
                    DEFAULT_THRESHOLD
            );
        }

        /*
         * TEMPORARY DEVELOPMENT SIMULATION.
         *
         * This is NOT speaker recognition.
         *
         * Person 2 will replace this with:
         *
         * audio
         *   ↓
         * preprocessing
         *   ↓
         * speaker embedding
         *   ↓
         * cosine similarity
         *   ↓
         * verification decision
         */

        double similarityScore = 0.85;

        boolean match =
                similarityScore >= DEFAULT_THRESHOLD;

        SpeakerVerificationStatus status =
                match
                        ? SpeakerVerificationStatus.MATCH
                        : SpeakerVerificationStatus.MISMATCH;

        return new SpeakerVerifyResponse(
                speakerId,
                status,
                similarityScore,
                match,
                DEFAULT_THRESHOLD
        );
    }
}
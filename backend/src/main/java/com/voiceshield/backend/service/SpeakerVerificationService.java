package com.voiceshield.backend.service;

import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SpeakerVerificationService {

    private static final Logger log = LoggerFactory.getLogger(SpeakerVerificationService.class);
    private static final double SIMILARITY_THRESHOLD = 0.75;
    private static final double SUSPICIOUS_THRESHOLD = 0.50;

    // In-memory enrolled voice profiles (speakerId -> enrolled audio size / signature)
    private final Map<String, Integer> enrolledSpeakers = new ConcurrentHashMap<>();

    public SpeakerVerificationService() {
        // Pre-enroll default test profiles for SIH demo
        enrolledSpeakers.put("CEO-EXECUTIVE-101", 128);
        enrolledSpeakers.put("CFO-FINANCE-202", 256);
        enrolledSpeakers.put("DEMO-USER-001", 64);
    }

    public boolean enrollSpeaker(String speakerId, byte[] audioBytes) {
        if (speakerId == null || speakerId.trim().isEmpty()) {
            return false;
        }
        enrolledSpeakers.put(speakerId.trim(), audioBytes != null ? audioBytes.length : 128);
        log.info("Successfully enrolled voice biometric profile for speakerId: {}", speakerId);
        return true;
    }

    public SpeakerVerifyResponse verifySpeaker(String claimedSpeakerId, byte[] audioBytes) {
        if (claimedSpeakerId == null || !enrolledSpeakers.containsKey(claimedSpeakerId.trim())) {
            log.warn("Speaker ID '{}' not enrolled in biometric database", claimedSpeakerId);
            return new SpeakerVerifyResponse(
                    claimedSpeakerId,
                    SpeakerVerificationStatus.UNAVAILABLE,
                    null,
                    false,
                    SIMILARITY_THRESHOLD
            );
        }

        // Calculate biometric similarity
        double similarity = 0.88; // Default genuine match for enrolled voice
        if (audioBytes != null && audioBytes.length > 0) {
            // Heuristic check for demonstration
            int enrolledSignature = enrolledSpeakers.get(claimedSpeakerId.trim());
            int diff = Math.abs(audioBytes.length - enrolledSignature);
            if (diff % 17 == 0) {
                similarity = 0.28; // Simulated mismatch/attacker
            } else if (diff % 7 == 0) {
                similarity = 0.62; // Simulated suspicious deviation
            }
        }

        SpeakerVerificationStatus status;
        boolean isMatch;

        if (similarity >= SIMILARITY_THRESHOLD) {
            status = SpeakerVerificationStatus.MATCH;
            isMatch = true;
        } else if (similarity >= SUSPICIOUS_THRESHOLD) {
            status = SpeakerVerificationStatus.SUSPICIOUS_DEVIATION;
            isMatch = false;
        } else {
            status = SpeakerVerificationStatus.MISMATCH;
            isMatch = false;
        }

        return new SpeakerVerifyResponse(claimedSpeakerId, status, similarity, isMatch, SIMILARITY_THRESHOLD);
    }
}

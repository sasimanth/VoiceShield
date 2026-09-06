package com.voiceshield.backend.service;

import com.voiceshield.backend.dto.SpeakerVerifyResponse;
import com.voiceshield.backend.entity.SpeakerProfile;
import com.voiceshield.risk.model.SpeakerVerificationStatus;
import org.springframework.stereotype.Service;

@Service
public class SpeakerVerificationService {

    private final SpeakerProfileService speakerProfileService;

    public SpeakerVerificationService(SpeakerProfileService speakerProfileService) {
        this.speakerProfileService = speakerProfileService;
    }

    public boolean enrollSpeaker(String speakerId, byte[] audioBytes) {
        speakerProfileService.saveOrUpdateProfile(speakerId, audioBytes);
        return true;
    }

    public SpeakerVerifyResponse verifySpeaker(String speakerId, byte[] audioBytes) {
        SpeakerProfile profile = speakerProfileService.getSpeakerProfile(speakerId);

        double similarityScore = 0.85;
        double threshold = 0.75;
        boolean isMatch = similarityScore >= threshold;

        SpeakerVerificationStatus status = isMatch ? SpeakerVerificationStatus.MATCH : SpeakerVerificationStatus.MISMATCH;

        return new SpeakerVerifyResponse(
                profile.getSpeakerId(),
                status,
                similarityScore,
                isMatch,
                threshold
        );
    }
}
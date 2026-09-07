package com.voiceshield.backend.service;

import com.voiceshield.backend.entity.SpeakerProfile;
import com.voiceshield.backend.exception.ResourceNotFoundException;
import com.voiceshield.backend.repository.SpeakerProfileRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SpeakerProfileService {

    private final SpeakerProfileRepository speakerProfileRepository;

    public SpeakerProfileService(SpeakerProfileRepository speakerProfileRepository) {
        this.speakerProfileRepository = speakerProfileRepository;
    }

    @Cacheable(value = "speakerProfiles", key = "#speakerId")
    public SpeakerProfile getSpeakerProfile(String speakerId) {
        return speakerProfileRepository.findBySpeakerId(speakerId)
                .orElseThrow(() -> new ResourceNotFoundException("Speaker profile not found for ID: " + speakerId));
    }

    @CacheEvict(value = "speakerProfiles", key = "#speakerId")
    public void saveOrUpdateProfile(String speakerId, byte[] audioBytes) {
        SpeakerProfile profile = speakerProfileRepository.findBySpeakerId(speakerId)
                .orElse(new SpeakerProfile(speakerId, audioBytes));

        profile.setAudioData(audioBytes);
        profile.setUpdatedAt(LocalDateTime.now());
        speakerProfileRepository.save(profile);
    }
}
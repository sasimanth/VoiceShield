package com.voiceshield.backend.service;

import com.voiceshield.backend.entity.SpeakerProfile;
import com.voiceshield.backend.exception.ResourceNotFoundException;
import com.voiceshield.backend.repository.SpeakerProfileRepository;
import com.voiceshield.backend.util.VectorUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SpeakerProfileService {

    private final SpeakerProfileRepository speakerProfileRepository;

    public SpeakerProfileService(SpeakerProfileRepository speakerProfileRepository) {
        this.speakerProfileRepository = speakerProfileRepository;
    }

    @Cacheable(value = "speakerProfiles", key = "#speakerId")
    @Transactional(readOnly = true)
    public SpeakerProfile getSpeakerProfile(String speakerId) {
        return speakerProfileRepository.findBySpeakerId(speakerId)
                .orElseThrow(() -> new ResourceNotFoundException("Speaker profile not found for ID: " + speakerId));
    }

    @Transactional(readOnly = true)
    public Optional<SpeakerProfile> findSpeakerProfile(String speakerId) {
        return speakerProfileRepository.findBySpeakerId(speakerId);
    }

    @CacheEvict(value = "speakerProfiles", key = "#speakerId")
    public void saveOrUpdateProfile(String speakerId, byte[] audioBytes) {
        SpeakerProfile profile = speakerProfileRepository.findBySpeakerId(speakerId)
                .orElse(new SpeakerProfile(speakerId, audioBytes));

        profile.setAudioData(audioBytes);
        profile.setUpdatedAt(LocalDateTime.now());
        speakerProfileRepository.save(profile);
    }

    @CacheEvict(value = "speakerProfiles", key = "#speakerId")
    public void saveOrUpdateEmbeddingProfile(String speakerId, float[] embedding) {
        byte[] serializedVector = VectorUtils.floatArrayToBytes(embedding);
        saveOrUpdateProfile(speakerId, serializedVector);
    }

    @Transactional(readOnly = true)
    public float[] getSpeakerEmbedding(String speakerId) {
        Optional<SpeakerProfile> optionalProfile = findSpeakerProfile(speakerId);
        if (optionalProfile.isEmpty()) {
            return null;
        }
        byte[] rawBytes = optionalProfile.get().getAudioData();
        return VectorUtils.bytesToFloatArray(rawBytes);
    }
}
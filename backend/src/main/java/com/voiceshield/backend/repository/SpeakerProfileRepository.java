package com.voiceshield.backend.repository;

import com.voiceshield.backend.entity.SpeakerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpeakerProfileRepository extends JpaRepository<SpeakerProfile, Long> {

    Optional<SpeakerProfile> findBySpeakerId(String speakerId);

    boolean existsBySpeakerId(String speakerId);
}
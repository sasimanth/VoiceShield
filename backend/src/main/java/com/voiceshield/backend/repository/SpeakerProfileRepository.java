package com.voiceshield.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.voiceshield.backend.entity.SpeakerProfile;

@Repository
public interface SpeakerProfileRepository extends JpaRepository<SpeakerProfile, Long> {
    Optional<SpeakerProfile> findBySpeakerId(String speakerId);
}
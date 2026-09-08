package com.voiceshield.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "speaker_profiles")
public class SpeakerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "speaker_id", nullable = false, unique = true)
    private String speakerId;

    @Column(name = "audio_data")
    private byte[] audioData;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SpeakerProfile() {}

    public SpeakerProfile(String speakerId, byte[] audioData) {
        this.speakerId = speakerId;
        this.audioData = audioData;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { 
        return id; 
    }

    public String getSpeakerId() { 
        return speakerId; 
    }

    public void setSpeakerId(String speakerId) { 
        this.speakerId = speakerId; 
    }

    public byte[] getAudioData() { 
        return audioData; 
    }

    public void setAudioData(byte[] audioData) { 
        this.audioData = audioData; 
    }

    public LocalDateTime getUpdatedAt() { 
        return updatedAt; 
    }

    public void setUpdatedAt(LocalDateTime updatedAt) { 
        this.updatedAt = updatedAt; 
    }
}
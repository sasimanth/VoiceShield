package com.voiceshield.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "speaker_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_speaker_profiles_speaker_id", columnNames = "speaker_id")
        },
        indexes = {
                @Index(name = "idx_speaker_profiles_speaker_id", columnList = "speaker_id")
        }
)
public class SpeakerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "speaker_id", nullable = false, unique = true, length = 100)
    private String speakerId;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "audio_data", nullable = false, columnDefinition = "bytea")
    private byte[] audioData;

    @Column(name = "embedding_dimension", nullable = false)
    private Integer embeddingDimension = 192;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SpeakerProfile() {
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (embeddingDimension == null) {
            embeddingDimension = 192;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public void setAudioData(byte[] audioData) {
        this.audioData = audioData;
    }

    public Integer getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(Integer embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
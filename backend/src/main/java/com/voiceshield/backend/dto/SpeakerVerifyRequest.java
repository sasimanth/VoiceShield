package com.voiceshield.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public class SpeakerVerifyRequest {

    @NotBlank(message = "Claimed Speaker ID is required")
    @JsonProperty("claimed_speaker_id")
    private String claimedSpeakerId;

    @NotBlank(message = "Audio data is required")
@JsonProperty("audio_base64")
private String audioBase64;

    public SpeakerVerifyRequest() {}

    public SpeakerVerifyRequest(String claimedSpeakerId, String audioBase64) {
        this.claimedSpeakerId = claimedSpeakerId;
        this.audioBase64 = audioBase64;
    }

    public String getClaimedSpeakerId() { return claimedSpeakerId; }
    public void setClaimedSpeakerId(String claimedSpeakerId) { this.claimedSpeakerId = claimedSpeakerId; }

    public String getAudioBase64() { return audioBase64; }
    public void setAudioBase64(String audioBase64) { this.audioBase64 = audioBase64; }
}

package com.voiceshield.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class SpeakerEnrollRequest {

    @NotBlank(message = "Speaker ID is required")
    @JsonProperty("speaker_id")
    private String speakerId;

    @JsonProperty("audio_base64")
    private String audioBase64;

    public SpeakerEnrollRequest() {}

    public SpeakerEnrollRequest(String speakerId, String audioBase64) {
        this.speakerId = speakerId;
        this.audioBase64 = audioBase64;
    }

    public String getSpeakerId() { return speakerId; }
    public void setSpeakerId(String speakerId) { this.speakerId = speakerId; }

    public String getAudioBase64() { return audioBase64; }
    public void setAudioBase64(String audioBase64) { this.audioBase64 = audioBase64; }
}

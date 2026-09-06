package com.voiceshield.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpeakerEnrollResponse {

    private boolean success;

    @JsonProperty("speaker_id")
    private String speakerId;

    private String message;

    public SpeakerEnrollResponse() {
    }

    public SpeakerEnrollResponse(boolean success, String speakerId, String message) {
        this.success = success;
        this.speakerId = speakerId;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSpeakerId() {
        return speakerId;
    }

    public void setSpeakerId(String speakerId) {
        this.speakerId = speakerId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
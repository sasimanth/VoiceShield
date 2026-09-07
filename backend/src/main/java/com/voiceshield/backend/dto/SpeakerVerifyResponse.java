package com.voiceshield.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.voiceshield.risk.model.SpeakerVerificationStatus;

public class SpeakerVerifyResponse {

    @JsonProperty("speaker_id")
    private String speakerId;

    @JsonProperty("status")
    private SpeakerVerificationStatus status;

    @JsonProperty("similarity_score")
    private Double similarityScore;

    @JsonProperty("is_match")
    private boolean isMatch;

    @JsonProperty("threshold")
    private Double threshold;

    public SpeakerVerifyResponse() {}

    public SpeakerVerifyResponse(String speakerId, SpeakerVerificationStatus status, Double similarityScore, boolean isMatch, Double threshold) {
        this.speakerId = speakerId;
        this.status = status;
        this.similarityScore = similarityScore;
        this.isMatch = isMatch;
        this.threshold = threshold;
    }

    public String getSpeakerId() { return speakerId; }
    public void setSpeakerId(String speakerId) { this.speakerId = speakerId; }

    public SpeakerVerificationStatus getStatus() { return status; }
    public void setStatus(SpeakerVerificationStatus status) { this.status = status; }

    public Double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }

    public boolean isMatch() { return isMatch; }
    public void setMatch(boolean match) { isMatch = match; }

    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }
}

package com.voiceshield.risk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standardized input payload consumed by Person 4's Risk Engine from Person 3.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskSignalInput {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("caller_id")
    private String callerId;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("nonce")
    private String nonce;

    @JsonProperty("deepfake_probability")
    private Double deepfakeProbability;

    @JsonProperty("speaker_verification_status")
    private SpeakerVerificationStatus speakerVerificationStatus = SpeakerVerificationStatus.UNAVAILABLE;

    @JsonProperty("speaker_similarity")
    private Double speakerSimilarity;

    @JsonProperty("acoustic_anomaly")
    private Double acousticAnomaly;

    @JsonProperty("prosodic_anomaly")
    private Double prosodicAnomaly;

    @JsonProperty("behavioral_anomaly")
    private Double behavioralAnomaly;

    @JsonProperty("context")
    private ContextMetadata context;

    public RiskSignalInput() {
    }

    // Builder-like convenience constructor for testing and mocks
    public RiskSignalInput(Double deepfakeProbability,
                           SpeakerVerificationStatus speakerVerificationStatus,
                           Double speakerSimilarity,
                           ContextMetadata context) {
        this.deepfakeProbability = deepfakeProbability;
        this.speakerVerificationStatus = speakerVerificationStatus;
        this.speakerSimilarity = speakerSimilarity;
        this.context = context;
    }

    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Double getDeepfakeProbability() {
        return deepfakeProbability;
    }

    public void setDeepfakeProbability(Double deepfakeProbability) {
        this.deepfakeProbability = deepfakeProbability;
    }

    public SpeakerVerificationStatus getSpeakerVerificationStatus() {
        return speakerVerificationStatus;
    }

    public void setSpeakerVerificationStatus(SpeakerVerificationStatus speakerVerificationStatus) {
        this.speakerVerificationStatus = speakerVerificationStatus;
    }

    public Double getSpeakerSimilarity() {
        return speakerSimilarity;
    }

    public void setSpeakerSimilarity(Double speakerSimilarity) {
        this.speakerSimilarity = speakerSimilarity;
    }

    public Double getAcousticAnomaly() {
        return acousticAnomaly;
    }

    public void setAcousticAnomaly(Double acousticAnomaly) {
        this.acousticAnomaly = acousticAnomaly;
    }

    public Double getProsodicAnomaly() {
        return prosodicAnomaly;
    }

    public void setProsodicAnomaly(Double prosodicAnomaly) {
        this.prosodicAnomaly = prosodicAnomaly;
    }

    public Double getBehavioralAnomaly() {
        return behavioralAnomaly;
    }

    public void setBehavioralAnomaly(Double behavioralAnomaly) {
        this.behavioralAnomaly = behavioralAnomaly;
    }

    public ContextMetadata getContext() {
        return context;
    }

    public void setContext(ContextMetadata context) {
        this.context = context;
    }
}

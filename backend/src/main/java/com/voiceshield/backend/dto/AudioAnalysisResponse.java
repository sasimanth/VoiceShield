package com.voiceshield.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.voiceshield.risk.model.Decision;
import com.voiceshield.risk.model.RiskEvaluationResult;
import com.voiceshield.risk.model.RiskLevel;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AudioAnalysisResponse {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("risk_score")
    private int riskScore;

    @JsonProperty("risk_level")
    private RiskLevel riskLevel;

    @JsonProperty("decision")
    private Decision decision;

    @JsonProperty("reasons")
    private List<String> reasons;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("component_breakdown")
    private Map<String, Object> componentBreakdown;

    @JsonProperty("security_metadata")
    private Map<String, Object> securityMetadata;

    @JsonProperty("raw_ml_features")
    private Map<String, Object> rawMlFeatures;

    public AudioAnalysisResponse() {}

    public AudioAnalysisResponse(RiskEvaluationResult evalResult, String sessionId, Map<String, Object> rawMlFeatures) {
        if (evalResult != null) {
            this.riskScore = evalResult.getRiskScore();
            this.riskLevel = evalResult.getRiskLevel();
            this.decision = evalResult.getDecision();
            this.reasons = evalResult.getReasons();
            this.modelVersion = evalResult.getModelVersion();
            this.timestamp = evalResult.getTimestamp();
            this.componentBreakdown = evalResult.getComponentBreakdown();
            this.securityMetadata = evalResult.getSecurityMetadata();
        }
        this.sessionId = sessionId;
        this.rawMlFeatures = rawMlFeatures;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public Decision getDecision() { return decision; }
    public void setDecision(Decision decision) { this.decision = decision; }

    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> reasons) { this.reasons = reasons; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getComponentBreakdown() { return componentBreakdown; }
    public void setComponentBreakdown(Map<String, Object> componentBreakdown) { this.componentBreakdown = componentBreakdown; }

    public Map<String, Object> getSecurityMetadata() { return securityMetadata; }
    public void setSecurityMetadata(Map<String, Object> securityMetadata) { this.securityMetadata = securityMetadata; }

    public Map<String, Object> getRawMlFeatures() { return rawMlFeatures; }
    public void setRawMlFeatures(Map<String, Object> rawMlFeatures) { this.rawMlFeatures = rawMlFeatures; }
}

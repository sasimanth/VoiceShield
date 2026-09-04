package com.voiceshield.risk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Standardized, frozen output contract defined in integration/contracts/risk_contract.schema.json.
 * Strict guarantee: risk_score is an integer from 0 to 100.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "risk_score",
        "risk_level",
        "decision",
        "reasons",
        "model_version",
        "timestamp",
        "component_breakdown",
        "security_metadata"
})
public class RiskEvaluationResult {

    @JsonProperty("risk_score")
    private int riskScore;

    @JsonProperty("risk_level")
    private RiskLevel riskLevel;

    @JsonProperty("decision")
    private Decision decision;

    @JsonProperty("reasons")
    private List<String> reasons = new ArrayList<>();

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("component_breakdown")
    private Map<String, Object> componentBreakdown;

    @JsonProperty("security_metadata")
    private Map<String, Object> securityMetadata;

    public RiskEvaluationResult() {
    }

    public RiskEvaluationResult(int riskScore,
                                RiskLevel riskLevel,
                                Decision decision,
                                List<String> reasons,
                                String modelVersion,
                                String timestamp) {
        this.riskScore = Math.max(0, Math.min(100, riskScore));
        this.riskLevel = riskLevel;
        this.decision = decision;
        this.reasons = reasons != null ? new ArrayList<>(reasons) : new ArrayList<>();
        this.modelVersion = modelVersion;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = Math.max(0, Math.min(100, riskScore));
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons != null ? new ArrayList<>(reasons) : new ArrayList<>();
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getComponentBreakdown() {
        return componentBreakdown;
    }

    public void setComponentBreakdown(Map<String, Object> componentBreakdown) {
        this.componentBreakdown = componentBreakdown;
    }

    public Map<String, Object> getSecurityMetadata() {
        return securityMetadata;
    }

    public void setSecurityMetadata(Map<String, Object> securityMetadata) {
        this.securityMetadata = securityMetadata;
    }
}

package com.voiceshield.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MlInferenceResponse {

    @JsonProperty("deepfake_score")
    private double deepfakeScore;

    @JsonProperty("analysis_status")
    private String analysisStatus; // "OPTIMAL" or "DEGRADED"

    @JsonProperty("ml_service_status")
    private String mlServiceStatus; // "HEALTHY" or "UNAVAILABLE"

    public MlInferenceResponse() {}

    public MlInferenceResponse(double deepfakeScore, String analysisStatus, String mlServiceStatus) {
        this.deepfakeScore = deepfakeScore;
        this.analysisStatus = analysisStatus;
        this.mlServiceStatus = mlServiceStatus;
    }

    public double getDeepfakeScore() {
        return deepfakeScore;
    }

    public void setDeepfakeScore(double deepfakeScore) {
        this.deepfakeScore = deepfakeScore;
    }

    public String getAnalysisStatus() {
        return analysisStatus;
    }

    public void setAnalysisStatus(String analysisStatus) {
        this.analysisStatus = analysisStatus;
    }

    public String getMlServiceStatus() {
        return mlServiceStatus;
    }

    public void setMlServiceStatus(String mlServiceStatus) {
        this.mlServiceStatus = mlServiceStatus;
    }
}
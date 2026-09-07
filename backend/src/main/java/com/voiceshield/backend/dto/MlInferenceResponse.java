package com.voiceshield.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MlInferenceResponse {

    @JsonProperty("deepfake_score")
    @JsonAlias({"deepfake_probability", "deepfake_score"})
    private double deepfakeScore;

    @JsonProperty("acoustic_anomaly")
    private double acousticAnomaly;

    @JsonProperty("prosodic_anomaly")
    private double prosodicAnomaly;

    @JsonProperty("behavioral_anomaly")
    private double behavioralAnomaly;

    @JsonProperty("analysis_status")
    private String analysisStatus; // "OPTIMAL" or "DEGRADED"

    @JsonProperty("ml_service_status")
    private String mlServiceStatus; // "HEALTHY" or "UNAVAILABLE"

    public MlInferenceResponse() {}

    public MlInferenceResponse(double deepfakeScore, double acousticAnomaly, double prosodicAnomaly, double behavioralAnomaly, String analysisStatus, String mlServiceStatus) {
        this.deepfakeScore = deepfakeScore;
        this.acousticAnomaly = acousticAnomaly;
        this.prosodicAnomaly = prosodicAnomaly;
        this.behavioralAnomaly = behavioralAnomaly;
        this.analysisStatus = analysisStatus;
        this.mlServiceStatus = mlServiceStatus;
    }

    public static MlInferenceResponse degraded() {
        return new MlInferenceResponse(0.0, 0.0, 0.0, 0.0, "DEGRADED", "UNAVAILABLE");
    }

    public double getDeepfakeScore() {
        return deepfakeScore;
    }

    public void setDeepfakeScore(double deepfakeScore) {
        this.deepfakeScore = deepfakeScore;
    }

    public double getAcousticAnomaly() {
        return acousticAnomaly;
    }

    public void setAcousticAnomaly(double acousticAnomaly) {
        this.acousticAnomaly = acousticAnomaly;
    }

    public double getProsodicAnomaly() {
        return prosodicAnomaly;
    }

    public void setProsodicAnomaly(double prosodicAnomaly) {
        this.prosodicAnomaly = prosodicAnomaly;
    }

    public double getBehavioralAnomaly() {
        return behavioralAnomaly;
    }

    public void setBehavioralAnomaly(double behavioralAnomaly) {
        this.behavioralAnomaly = behavioralAnomaly;
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
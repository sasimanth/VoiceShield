package com.voiceshield.risk.risk;

/**
 * Modular configuration for Multi-Signal Risk Weights and Thresholds.
 * Enables Person 4 to modify signal importance without refactoring engine logic.
 */
public class RiskWeightsConfig {

    // Default Baseline Weights (Sum = 1.0)
    private double deepfakeWeight = 0.40;
    private double acousticWeight = 0.15;
    private double prosodicWeight = 0.15;
    private double speakerMismatchWeight = 0.15;
    private double contextWeight = 0.15;

    // Fallback Weights when Speaker Verification is UNAVAILABLE (Sum = 1.0)
    private double fallbackDeepfakeWeight = 0.50;
    private double fallbackAcousticWeight = 0.18;
    private double fallbackProsodicWeight = 0.17;
    private double fallbackContextWeight = 0.15;

    // Decision Thresholds (Scale: 0 - 100)
    private int criticalThreshold = 75;
    private int highThreshold = 50;
    private int mediumThreshold = 25;

    // Circuit Breakers / Override Rules
    private double autoBlockDeepfakeThreshold = 0.90;
    private double autoBlockContextThreshold = 0.70;

    public static RiskWeightsConfig createDefault() {
        return new RiskWeightsConfig();
    }

    // Getters and Setters
    public double getDeepfakeWeight() {
        return deepfakeWeight;
    }

    public void setDeepfakeWeight(double deepfakeWeight) {
        this.deepfakeWeight = deepfakeWeight;
    }

    public double getAcousticWeight() {
        return acousticWeight;
    }

    public void setAcousticWeight(double acousticWeight) {
        this.acousticWeight = acousticWeight;
    }

    public double getProsodicWeight() {
        return prosodicWeight;
    }

    public void setProsodicWeight(double prosodicWeight) {
        this.prosodicWeight = prosodicWeight;
    }

    public double getSpeakerMismatchWeight() {
        return speakerMismatchWeight;
    }

    public void setSpeakerMismatchWeight(double speakerMismatchWeight) {
        this.speakerMismatchWeight = speakerMismatchWeight;
    }

    public double getContextWeight() {
        return contextWeight;
    }

    public void setContextWeight(double contextWeight) {
        this.contextWeight = contextWeight;
    }

    public double getFallbackDeepfakeWeight() {
        return fallbackDeepfakeWeight;
    }

    public void setFallbackDeepfakeWeight(double fallbackDeepfakeWeight) {
        this.fallbackDeepfakeWeight = fallbackDeepfakeWeight;
    }

    public double getFallbackAcousticWeight() {
        return fallbackAcousticWeight;
    }

    public void setFallbackAcousticWeight(double fallbackAcousticWeight) {
        this.fallbackAcousticWeight = fallbackAcousticWeight;
    }

    public double getFallbackProsodicWeight() {
        return fallbackProsodicWeight;
    }

    public void setFallbackProsodicWeight(double fallbackProsodicWeight) {
        this.fallbackProsodicWeight = fallbackProsodicWeight;
    }

    public double getFallbackContextWeight() {
        return fallbackContextWeight;
    }

    public void setFallbackContextWeight(double fallbackContextWeight) {
        this.fallbackContextWeight = fallbackContextWeight;
    }

    public int getCriticalThreshold() {
        return criticalThreshold;
    }

    public void setCriticalThreshold(int criticalThreshold) {
        this.criticalThreshold = criticalThreshold;
    }

    public int getHighThreshold() {
        return highThreshold;
    }

    public void setHighThreshold(int highThreshold) {
        this.highThreshold = highThreshold;
    }

    public int getMediumThreshold() {
        return mediumThreshold;
    }

    public void setMediumThreshold(int mediumThreshold) {
        this.mediumThreshold = mediumThreshold;
    }

    public double getAutoBlockDeepfakeThreshold() {
        return autoBlockDeepfakeThreshold;
    }

    public void setAutoBlockDeepfakeThreshold(double autoBlockDeepfakeThreshold) {
        this.autoBlockDeepfakeThreshold = autoBlockDeepfakeThreshold;
    }

    public double getAutoBlockContextThreshold() {
        return autoBlockContextThreshold;
    }

    public void setAutoBlockContextThreshold(double autoBlockContextThreshold) {
        this.autoBlockContextThreshold = autoBlockContextThreshold;
    }
}

package com.voiceshield.risk.context;

/**
 * Modular configuration for Contextual Fraud Rules & Thresholds.
 * Enables Person 4 / Risk Engineers to tune weights without core logic rewrite.
 */
public class ContextRulesConfig {

    private double highTransactionThreshold = 500000.0; // ₹5 Lakhs
    private double mediumTransactionThreshold = 100000.0; // ₹1 Lakh
    private double highTransactionWeight = 0.35;
    private double mediumTransactionWeight = 0.20;

    private double urgencyWeight = 0.25;
    private double newBeneficiaryWeight = 0.20;
    private double internationalCallWeight = 0.15;
    private double lowCallerTrustWeight = 0.15;
    private double privilegedAccountWeight = 0.15;

    private double secondaryAuthContextThreshold = 0.40;

    public static ContextRulesConfig createDefault() {
        return new ContextRulesConfig();
    }

    // Getters and Setters
    public double getHighTransactionThreshold() {
        return highTransactionThreshold;
    }

    public void setHighTransactionThreshold(double highTransactionThreshold) {
        this.highTransactionThreshold = highTransactionThreshold;
    }

    public double getMediumTransactionThreshold() {
        return mediumTransactionThreshold;
    }

    public void setMediumTransactionThreshold(double mediumTransactionThreshold) {
        this.mediumTransactionThreshold = mediumTransactionThreshold;
    }

    public double getHighTransactionWeight() {
        return highTransactionWeight;
    }

    public void setHighTransactionWeight(double highTransactionWeight) {
        this.highTransactionWeight = highTransactionWeight;
    }

    public double getMediumTransactionWeight() {
        return mediumTransactionWeight;
    }

    public void setMediumTransactionWeight(double mediumTransactionWeight) {
        this.mediumTransactionWeight = mediumTransactionWeight;
    }

    public double getUrgencyWeight() {
        return urgencyWeight;
    }

    public void setUrgencyWeight(double urgencyWeight) {
        this.urgencyWeight = urgencyWeight;
    }

    public double getNewBeneficiaryWeight() {
        return newBeneficiaryWeight;
    }

    public void setNewBeneficiaryWeight(double newBeneficiaryWeight) {
        this.newBeneficiaryWeight = newBeneficiaryWeight;
    }

    public double getInternationalCallWeight() {
        return internationalCallWeight;
    }

    public void setInternationalCallWeight(double internationalCallWeight) {
        this.internationalCallWeight = internationalCallWeight;
    }

    public double getLowCallerTrustWeight() {
        return lowCallerTrustWeight;
    }

    public void setLowCallerTrustWeight(double lowCallerTrustWeight) {
        this.lowCallerTrustWeight = lowCallerTrustWeight;
    }

    public double getPrivilegedAccountWeight() {
        return privilegedAccountWeight;
    }

    public void setPrivilegedAccountWeight(double privilegedAccountWeight) {
        this.privilegedAccountWeight = privilegedAccountWeight;
    }

    public double getSecondaryAuthContextThreshold() {
        return secondaryAuthContextThreshold;
    }

    public void setSecondaryAuthContextThreshold(double secondaryAuthContextThreshold) {
        this.secondaryAuthContextThreshold = secondaryAuthContextThreshold;
    }
}

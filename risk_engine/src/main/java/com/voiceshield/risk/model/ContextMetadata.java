package com.voiceshield.risk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contextual and behavioral parameters surrounding the voice session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContextMetadata {

    @JsonProperty("transaction_amount")
    private Double transactionAmount;

    @JsonProperty("currency")
    private String currency = "INR";

    @JsonProperty("is_new_beneficiary")
    private boolean isNewBeneficiary;

    @JsonProperty("is_international_call")
    private boolean isInternationalCall;

    @JsonProperty("urgency")
    private boolean urgency;

    @JsonProperty("caller_trust_score")
    private Double callerTrustScore = 1.0;

    @JsonProperty("is_privileged_account")
    private boolean isPrivilegedAccount;

    @JsonProperty("failed_auth_attempts")
    private int failedAuthAttempts = 0;

    public ContextMetadata() {
    }

    public ContextMetadata(Double transactionAmount, boolean isNewBeneficiary, boolean isInternationalCall,
                           boolean urgency, Double callerTrustScore) {
        this.transactionAmount = transactionAmount;
        this.isNewBeneficiary = isNewBeneficiary;
        this.isInternationalCall = isInternationalCall;
        this.urgency = urgency;
        this.callerTrustScore = callerTrustScore;
    }

    // Getters and Setters
    public Double getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(Double transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isNewBeneficiary() {
        return isNewBeneficiary;
    }

    public void setNewBeneficiary(boolean newBeneficiary) {
        isNewBeneficiary = newBeneficiary;
    }

    public boolean isInternationalCall() {
        return isInternationalCall;
    }

    public void setInternationalCall(boolean internationalCall) {
        isInternationalCall = internationalCall;
    }

    public boolean isUrgency() {
        return urgency;
    }

    public void setUrgency(boolean urgency) {
        this.urgency = urgency;
    }

    public Double getCallerTrustScore() {
        return callerTrustScore;
    }

    public void setCallerTrustScore(Double callerTrustScore) {
        this.callerTrustScore = callerTrustScore;
    }

    public boolean isPrivilegedAccount() {
        return isPrivilegedAccount;
    }

    public void setPrivilegedAccount(boolean privilegedAccount) {
        isPrivilegedAccount = privilegedAccount;
    }

    public int getFailedAuthAttempts() {
        return failedAuthAttempts;
    }

    public void setFailedAuthAttempts(int failedAuthAttempts) {
        this.failedAuthAttempts = failedAuthAttempts;
    }
}

package com.voiceshield.risk.context;

import com.voiceshield.risk.model.ContextAnalysisResult;
import com.voiceshield.risk.model.ContextMetadata;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stage 1: Context Analysis Engine.
 * Evaluates financial transaction values, social engineering pressure,
 * beneficiary novelty, and caller trust.
 * 
 * Strict Principle: Decoupled from core multi-signal risk calculation.
 * signals -> context analysis -> risk evaluation -> risk score -> decision
 */
public class ContextAnalyzer {

    private final ContextRulesConfig config;
    private final NumberFormat currencyFormatter;

    public ContextAnalyzer() {
        this(ContextRulesConfig.createDefault());
    }

    public ContextAnalyzer(ContextRulesConfig config) {
        this.config = config != null ? config : ContextRulesConfig.createDefault();
        this.currencyFormatter = NumberFormat.getCurrencyInstance(Locale.of("en", "IN"));
    }

    public ContextAnalysisResult analyze(ContextMetadata metadata) {
        if (metadata == null) {
            return new ContextAnalysisResult(0.0, false, List.of());
        }

        double contextScore = 0.0;
        List<String> threatFactors = new ArrayList<>();

        // 1. Transaction Amount Evaluation
        Double amount = metadata.getTransactionAmount();
        if (amount != null && amount > 0) {
            if (amount >= config.getHighTransactionThreshold()) {
                contextScore += config.getHighTransactionWeight();
                threatFactors.add("High-value fund transfer request (" + formatCurrency(amount, metadata.getCurrency()) + ")");
            } else if (amount >= config.getMediumTransactionThreshold()) {
                contextScore += config.getMediumTransactionWeight();
                threatFactors.add("Elevated fund transfer request (" + formatCurrency(amount, metadata.getCurrency()) + ")");
            }
        }

        // 2. Social Engineering Urgency / Pressure
        if (metadata.isUrgency()) {
            contextScore += config.getUrgencyWeight();
            threatFactors.add("High urgency / social engineering coercion pressure detected");
        }

        // 3. New / Non-whitelisted Beneficiary
        if (metadata.isNewBeneficiary()) {
            contextScore += config.getNewBeneficiaryWeight();
            threatFactors.add("Transfer requested to new / unverified beneficiary account");
        }

        // 4. Foreign / International Gateway Origin
        if (metadata.isInternationalCall()) {
            contextScore += config.getInternationalCallWeight();
            threatFactors.add("Foreign / VoIP unverified gateway origin");
        }

        // 5. Caller Reputation & Trust Score
        Double trustScore = metadata.getCallerTrustScore();
        if (trustScore != null && trustScore < 0.5) {
            contextScore += config.getLowCallerTrustWeight();
            threatFactors.add("Low caller trust score / untrusted contact (" + String.format("%.2f", trustScore) + ")");
        }

        // 6. Privileged Executive / Admin Account Targeting
        if (metadata.isPrivilegedAccount()) {
            contextScore += config.getPrivilegedAccountWeight();
            threatFactors.add("Session targets high-privilege corporate / executive account");
        }

        // 7. Repeated Failed Verification Attempts
        if (metadata.getFailedAuthAttempts() >= 2) {
            double authPenalty = Math.min(0.20, metadata.getFailedAuthAttempts() * 0.10);
            contextScore += authPenalty;
            threatFactors.add("Multiple prior authentication failures (" + metadata.getFailedAuthAttempts() + " attempts)");
        }

        // Cap context score strictly between 0.0 and 1.0
        double normalizedContext = Math.min(1.0, Math.max(0.0, contextScore));
        boolean requiresSecondary = normalizedContext >= config.getSecondaryAuthContextThreshold();

        return new ContextAnalysisResult(normalizedContext, requiresSecondary, threatFactors);
    }

    private String formatCurrency(double amount, String currency) {
        String curr = currency != null ? currency : "INR";
        if ("INR".equalsIgnoreCase(curr)) {
            return String.format("INR %,.2f", amount);
        }
        return String.format("%s %,.2f", curr, amount);
    }
}

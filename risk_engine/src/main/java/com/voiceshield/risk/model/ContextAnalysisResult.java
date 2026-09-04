package com.voiceshield.risk.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Output of the standalone Context Analysis stage.
 * Keeps context evaluation strictly decoupled from the core multi-signal risk formula.
 */
public class ContextAnalysisResult {

    private final double contextualRisk;
    private final boolean requiresSecondaryAuth;
    private final List<String> threatFactors;

    public ContextAnalysisResult(double contextualRisk, boolean requiresSecondaryAuth, List<String> threatFactors) {
        this.contextualRisk = Math.max(0.0, Math.min(1.0, contextualRisk));
        this.requiresSecondaryAuth = requiresSecondaryAuth;
        this.threatFactors = threatFactors != null ? new ArrayList<>(threatFactors) : new ArrayList<>();
    }

    public double getContextualRisk() {
        return contextualRisk;
    }

    public boolean isRequiresSecondaryAuth() {
        return requiresSecondaryAuth;
    }

    public List<String> getThreatFactors() {
        return threatFactors;
    }
}

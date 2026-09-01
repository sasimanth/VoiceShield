"""
Composite Real-Time Risk Scoring & Decision Engine
Owner: Person 4 (Risk & Cybersecurity Engineer)
"""

from typing import Dict, Any, List, Optional
from security.context_engine.context_analyzer import ContextAnalyzer


class RiskScoringEngine:
    """
    Computes a unified, explainable 0-100 Impersonation Risk Score combining:
    1. AI Deepfake / Synthetic Detection Probability (40% weight)
    2. Acoustic Spectral Anomaly Score (15% weight)
    3. Prosodic & Behavioral Anomaly Score (15% weight)
    4. Speaker Verification Identity Mismatch (15% weight)
    5. Contextual & Financial Threat Indicators (15% weight)
    """

    def __init__(self):
        self.context_analyzer = ContextAnalyzer()

    def evaluate(
        self,
        synthetic_prob: float,
        acoustic_anomaly: float,
        prosodic_anomaly: float,
        speaker_identity_anomaly: Optional[float] = None,
        context_metadata: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """Calculates multi-signal risk score and actionable response tier."""

        context_res = self.context_analyzer.evaluate(
            transaction_amount=context_metadata.get("transaction_amount") if context_metadata else None,
            is_international_call=context_metadata.get("is_international", False) if context_metadata else False,
            caller_trust_score=context_metadata.get("caller_trust_score", 1.0) if context_metadata else 1.0,
            urgency_flag=context_metadata.get("urgency_flag", False) if context_metadata else False,
            is_new_beneficiary=context_metadata.get("is_new_beneficiary", False) if context_metadata else False,
        )

        ctx_score = context_res["contextual_risk_score"]

        # If speaker verification is unavailable, re-weight among available signals
        if speaker_identity_anomaly is not None:
            raw_score = (
                (synthetic_prob * 0.40) +
                (acoustic_anomaly * 0.15) +
                (prosodic_anomaly * 0.15) +
                (speaker_identity_anomaly * 0.15) +
                (ctx_score * 0.15)
            )
        else:
            raw_score = (
                (synthetic_prob * 0.50) +
                (acoustic_anomaly * 0.18) +
                (prosodic_anomaly * 0.17) +
                (ctx_score * 0.15)
            )

        # Scale to 0 - 100 integer
        risk_score = int(round(raw_score * 100))
        risk_score = max(0, min(100, risk_score))

        # Determine Risk Tier and Actionable Recommendations
        if risk_score >= 75:
            risk_tier = "CRITICAL"
            action = "BLOCK_TRANSACTION"
            recommendations = [
                "IMMEDIATE ACTION: Do not authorize any financial disbursement or privileged access.",
                "AI Voice Clone detected with high confidence.",
                "Initiate mandatory out-of-band video call or in-person verification."
            ]
        elif risk_score >= 50:
            risk_tier = "HIGH"
            action = "STEP_UP_MFA"
            recommendations = [
                "Trigger Step-Up Multi-Factor Authentication (Hardware Token / Biometric Push).",
                "Execute manual call-back on official pre-registered number.",
                "Flag call session for cybersecurity SOC supervisor review."
            ]
        elif risk_score >= 25:
            risk_tier = "MEDIUM"
            action = "CAUTION_PROCEED"
            recommendations = [
                "Proceed with caution; verify verbal transaction details against ERP records.",
                "Monitor subsequent speech turns for prosodic anomalies."
            ]
        else:
            risk_tier = "LOW"
            action = "ALLOW"
            recommendations = [
                "Voice verified genuine human speech. No anomalies detected."
            ]

        return {
            "overall_risk_score": risk_score,
            "risk_tier": risk_tier,
            "action_required": action,
            "recommendations": recommendations,
            "component_scores": {
                "ai_synthetic_prob": round(synthetic_prob, 4),
                "acoustic_anomaly": round(acoustic_anomaly, 4),
                "prosodic_anomaly": round(prosodic_anomaly, 4),
                "speaker_identity_anomaly": round(speaker_identity_anomaly, 4) if speaker_identity_anomaly is not None else None,
                "contextual_risk": round(ctx_score, 4)
            },
            "context_threat_factors": context_res["threat_factors"]
        }

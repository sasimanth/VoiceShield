"""
Contextual Fraud & Transaction Risk Analysis Engine
Owner: Person 4 (Risk & Cybersecurity Engineer)
"""

from typing import Dict, Any, Optional


class ContextAnalyzer:
    """Evaluates banking transaction metadata, caller trust, and social engineering urgency."""

    def evaluate(
        self,
        transaction_amount: Optional[float] = None,
        is_international_call: bool = False,
        caller_trust_score: float = 1.0,
        urgency_flag: bool = False,
        is_new_beneficiary: bool = False
    ) -> Dict[str, Any]:
        """
        Computes a normalized contextual threat multiplier (0.0 to 1.0).
        """
        context_score = 0.0
        threat_factors = []

        # High financial value threshold (e.g. transactions > ₹1,00,000)
        if transaction_amount and transaction_amount >= 500000:
            context_score += 0.35
            threat_factors.append(f"High-value fund transfer request (₹{transaction_amount:,.2f})")
        elif transaction_amount and transaction_amount >= 100000:
            context_score += 0.20
            threat_factors.append(f"Elevated transfer request (₹{transaction_amount:,.2f})")

        # Urgency / Pressure indicator (common social engineering tactic)
        if urgency_flag:
            context_score += 0.25
            threat_factors.append("High urgency / social engineering pressure detected")

        # New / unverified beneficiary
        if is_new_beneficiary:
            context_score += 0.20
            threat_factors.append("New / non-whitelisted beneficiary account")

        # International caller spoof risk
        if is_international_call:
            context_score += 0.15
            threat_factors.append("Foreign / VoIP unverified gateway origin")

        # Caller reputation discount
        if caller_trust_score < 0.5:
            context_score += 0.15
            threat_factors.append("Low caller reputation / unknown contact")

        context_score = float(min(1.0, context_score))

        return {
            "contextual_risk_score": round(context_score, 4),
            "threat_factors": threat_factors,
            "requires_secondary_auth": context_score >= 0.40
        }

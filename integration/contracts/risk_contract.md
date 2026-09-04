# VoiceShield — Risk Engine Interface Contract

**Contract ID**: `integration/contracts/risk_contract.*`  
**Owner**: Person 4 (Risk & Cybersecurity Engineer)  
**Consumer**: Person 3 (Backend Engineer — FastAPI / PostgreSQL / Service Orchestration)  
**Status**: **FROZEN**

---

## 1. Scope & Guarantee
This contract defines the standardized input and output specification for the **VoiceShield Risk & Context Engine**.
- **Score Scale**: `0 - 100` strictly (integer). It is **NEVER** normalized to 0.0 - 1.0 in the final response.
- **Determinism**: Given the same input signals and context, the engine returns identical scores, tiers, and decision recommendations.
- **Graceful Fallback**: If speaker verification or optional signals are unavailable, the engine reweights remaining verified signals dynamically without failing or inventing fake numbers.

---

## 2. Standardized Output Schema

```json
{
  "risk_score": 0-100,
  "risk_level": "LOW|MEDIUM|HIGH|CRITICAL",
  "decision": "ALLOW|WARN|SECONDARY_VERIFICATION|BLOCK",
  "reasons": [
    "Explanation string 1",
    "Explanation string 2"
  ],
  "model_version": "v1.2.0-composite-risk",
  "timestamp": "2026-09-03T16:53:00Z",
  "component_breakdown": {
    "deepfake_probability": 0.92,
    "speaker_similarity": 0.38,
    "speaker_verification_status": "MISMATCH",
    "contextual_risk": 0.85,
    "behavioral_anomaly": 0.75,
    "acoustic_anomaly": 0.81,
    "prosodic_anomaly": 0.79
  },
  "security_metadata": {
    "session_hash": "a8f9c1b3d5e72901",
    "zero_retention_verified": true,
    "replay_nonce_valid": true
  }
}
```

---

## 3. Risk Thresholds & Classification Rules

| Composite Risk Score ($R$) | Risk Level (`risk_level`) | Actionable Decision (`decision`) | Policy Description |
| :--- | :--- | :--- | :--- |
| **$75 \le R \le 100$** | `CRITICAL` | `BLOCK` | Disallow voice-authorized action immediately. Alert SOC supervisor. Mandatory out-of-band video or branch verification. |
| **$50 \le R < 75$** | `HIGH` | `SECONDARY_VERIFICATION` | Step-up authentication required (Hardware Token / Push MFA / Out-of-band callback to pre-enrolled number). |
| **$25 \le R < 50$** | `MEDIUM` | `WARN` | Proceed with caution. Flag session for continuous sliding-window re-scoring. Verbally confirm transaction specifics. |
| **$0 \le R < 25$** | `LOW` | `ALLOW` | Voice verified genuine with low contextual fraud indicators. Authorization allowed to proceed. |

---

## 4. Input Signal Schema (Sent from Person 3 to Risk Engine)

```json
{
  "session_id": "call_sess_20260903_9941",
  "caller_id": "+91-9876543210",
  "timestamp": "2026-09-03T16:53:00Z",
  "nonce": "c4ca4238a0b92382",
  "signals": {
    "deepfake_probability": 0.87,
    "speaker_verification_status": "MATCH | SUSPICIOUS_DEVIATION | MISMATCH | UNAVAILABLE",
    "speaker_similarity": 0.82,
    "acoustic_anomaly": 0.34,
    "prosodic_anomaly": 0.41,
    "behavioral_anomaly": 0.65
  },
  "context": {
    "transaction_amount": 500000.00,
    "currency": "INR",
    "is_new_beneficiary": true,
    "is_international_call": false,
    "urgency": true,
    "caller_trust_score": 0.80
  }
}
```

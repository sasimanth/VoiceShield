# VoiceShield — Person 4 Engineering Handoff Document
**Role**: Person 4 (Risk & Cybersecurity Engineer)  
**Branch**: `member-sasimanth`  
**Target Consumer**: Person 3 (Backend Engineer — FastAPI / PostgreSQL / Service Orchestration)  
**Modules Delivered**: `risk_engine/` (Java 21+ / Maven / Jackson / JUnit 5)  
**Frozen Contract**: `integration/contracts/risk_contract.*`  

---

## 1. Risk Formula & Calculation Logic

### Conceptual Architecture
```
Incoming Signals (P1 / P2 / P3)
           │
           ▼
Stage 1: Context Analysis Engine  ──►  Contextual Risk Score (0.0 - 1.0) & Threat Factors
           │
           ▼
Stage 2: Multi-Signal Risk Engine ──►  Dynamic Signal Weighting & Penalty Math
           │
           ▼
Stage 3: Decision Engine          ──►  Risk Score (0 - 100), Tier & Actionable Decision
           │
           ▼
Standardized Output Contract      ──►  Person 3 (FastAPI Orchestration)
```

### Mathematical Formulation

#### Case A: Full 5-Signal Evaluation (Speaker Verification is Enrolled & Active)
When reference voice embeddings exist for the claimed speaker ID:

$$R_{\text{raw}} = (w_{\text{df}} \cdot P_{\text{synth}}) + (w_{\text{ac}} \cdot A_{\text{ac}}) + (w_{\text{pr}} \cdot A_{\text{pr}}) + (w_{\text{spk}} \cdot M_{\text{spk}}) + (w_{\text{ctx}} \cdot C_{\text{ctx}})$$

Where:
- $w_{\text{df}} = 0.40$ (AI Deepfake / Synthetic Speech Probability)
- $w_{\text{ac}} = 0.15$ (Acoustic / High-Frequency Spectral Anomaly)
- $w_{\text{pr}} = 0.15$ (Prosody / Monotone Pitch Cadence Anomaly)
- $w_{\text{spk}} = 0.15$ (Speaker Biometric Mismatch Score)
  - $M_{\text{spk}} = 0.0$ if status is `MATCH`
  - $M_{\text{spk}} = \max(0.0, 1.0 - S_{\text{similarity}})$ if status is `SUSPICIOUS_DEVIATION` or `MISMATCH`
- $w_{\text{ctx}} = 0.15$ (Contextual Threat Multiplier from Stage 1)
- **Sum of Weights**: $0.40 + 0.15 + 0.15 + 0.15 + 0.15 = 1.00$

#### Case B: Graceful Fallback (Speaker Verification is `UNAVAILABLE`)
If no reference voice profile exists, **the system strictly does NOT invent a fake speaker score**. The weights automatically rebalance across verified signals:

$$R_{\text{raw}} = (0.50 \cdot P_{\text{synth}}) + (0.18 \cdot A_{\text{ac}}) + (0.17 \cdot A_{\text{pr}}) + (0.15 \cdot C_{\text{ctx}})$$

- **Sum of Fallback Weights**: $0.50 + 0.18 + 0.17 + 0.15 = 1.00$

#### Scaling to Standard Integer:
$$R = \text{clamp}\Big(\text{round}(R_{\text{raw}} \times 100), \, 0, \, 100\Big)$$

---

## 2. Input Schema (Consumed from Person 3)

Frozen in JSON specification:

```json
{
  "session_id": "string (optional/UUID)",
  "caller_id": "string (E.164 phone or customer handle)",
  "timestamp": "string (ISO-8601 UTC)",
  "nonce": "string (anti-replay cryptographic token)",
  "deepfake_probability": 0.87,
  "speaker_verification_status": "MATCH | SUSPICIOUS_DEVIATION | MISMATCH | UNAVAILABLE",
  "speaker_similarity": 0.25,
  "acoustic_anomaly": 0.82,
  "prosodic_anomaly": 0.79,
  "behavioral_anomaly": 0.65,
  "context": {
    "transaction_amount": 500000.00,
    "currency": "INR",
    "is_new_beneficiary": true,
    "is_international_call": true,
    "urgency": true,
    "caller_trust_score": 0.80,
    "is_privileged_account": false,
    "failed_auth_attempts": 0
  }
}
```

---

## 3. Output Schema (Frozen Contract for Person 3)

Defined in [`integration/contracts/risk_contract.schema.json`](file:///d:/Projects/SIH/integration/contracts/risk_contract.schema.json):

```json
{
  "risk_score": 88,
  "risk_level": "CRITICAL",
  "decision": "BLOCK",
  "reasons": [
    "Critical AI voice cloning detected (synthetic_prob: 0.87)",
    "Speaker biometric mismatch against enrolled voice profile (similarity: 0.25)",
    "Severe spectral flux / vocoder high-frequency anomaly (score: 0.82)",
    "Monotone robotic pitch contour / prosodic cadence anomaly (score: 0.79)",
    "High-value fund transfer request (INR 500,000.00)",
    "High urgency / social engineering coercion pressure detected",
    "Foreign / VoIP unverified gateway origin"
  ],
  "model_version": "v1.2.0-composite-risk",
  "timestamp": "2026-09-03T16:58:00Z",
  "component_breakdown": {
    "deepfake_probability": 0.87,
    "speaker_verification_status": "MISMATCH",
    "speaker_similarity": 0.25,
    "contextual_risk": 0.75,
    "acoustic_anomaly": 0.82,
    "prosodic_anomaly": 0.79,
    "behavioral_anomaly": 0.65
  },
  "security_metadata": {
    "session_hash": "2930b73f0dccec4a",
    "zero_retention_verified": true,
    "replay_nonce_valid": true
  }
}
```

---

## 4. Thresholds & Classification Table

| Risk Score ($R$) | Risk Level (`risk_level`) | Actionable Decision (`decision`) | Recommended Security Protocol |
| :---: | :---: | :---: | :--- |
| **$75 \le R \le 100$** | `CRITICAL` | `BLOCK` | **DO NOT APPROVE**. Immediately terminate voice session authorization. Trigger alert to SOC fraud monitoring queue. |
| **$50 \le R < 75$** | `HIGH` | `SECONDARY_VERIFICATION` | **STEP-UP MFA**. Trigger hardware push notification, FIDO2 token, or automated out-of-band call-back to pre-registered number. |
| **$25 \le R < 50$** | `MEDIUM` | `WARN` | **CAUTION**. Proceed with caution; verbally confirm transfer details; flag session for continuous temporal sliding-window analysis. |
| **$0 \le R < 25$** | `LOW` | `ALLOW` | **APPROVE**. Voice verified as genuine human vocal tract resonance; low context threat factors. |

### Policy Overrides (Circuit Breakers):
1. **Critical Synthetic Threat**: If `deepfake_probability >= 0.90` AND `context_risk >= 0.70`, decision is forced to `BLOCK` (`CRITICAL`) regardless of other smoothing.
2. **Contextual Escalation**: If context metadata triggers `requires_secondary_auth` (e.g. transfer $\ge$ ₹5,00,000 to an unverified beneficiary), minimum decision is escalated to `SECONDARY_VERIFICATION`.

---

## 5. Example Scenarios

| Scenario | Input Signals | Score | Level | Decision | Forensic Rationale |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **1. Genuine Human** | $P_{\text{synth}}=0.04$, `MATCH` ($0.97$), ₹4,500 | **6** | `LOW` | `ALLOW` | Voice verified genuine human vocal tract resonance. |
| **2. Medium Warning** | $P_{\text{synth}}=0.35$, `SUSPICIOUS` ($0.62$), ₹45,000 | **35** | `MEDIUM` | `WARN` | Mild prosodic/acoustic anomalies detected. |
| **3. High Secondary** | $P_{\text{synth}}=0.68$, `MISMATCH` ($0.40$), ₹3,00,000, Urgency | **68** | `HIGH` | `SECONDARY_VERIFICATION` | Voice mismatch combined with high financial urgency. |
| **4. AI Clone Attack** | $P_{\text{synth}}=0.87$, `MISMATCH` ($0.25$), ₹5,00,000, Foreign VoIP | **88** | `CRITICAL` | `BLOCK` | Critical synthetic deepfake combined with severe fraud indicators. |
| **5. Contradictory Clone** | $P_{\text{synth}}=0.89$, `MATCH` ($0.94$), ₹7,50,000, Privileged VIP | **69** | `HIGH` | `SECONDARY_VERIFICATION` | **Alert**: Speaker matches profile but speech exhibits synthetic neural vocoder artifacts (targeted clone). |
| **6. Missing Reference** | $P_{\text{synth}}=0.22$, `UNAVAILABLE` ($null$), ₹50,000 | **11** | `LOW` | `ALLOW` | Dynamically rebalances without inventing fake similarity score. |

---

## 6. Verification & Automated Test Suite

Executed via Maven and JUnit 5:
```bash
cd risk_engine
mvn test
```
**Results: 34 Tests Run, 0 Failures, 0 Errors, 0 Skipped (Time: 1.88s)**:
- `ContextAnalyzerTest` (5 tests): Financial thresholds (₹1L, ₹5L), urgency, caller trust discounts, accumulated threat ceiling.
- `DecisionEngineTest` (10 tests): Exact boundary classification (0, 24, 25, 49, 50, 74, 75, 100), circuit-breakers, escalations.
- `RiskScoringEngineTest` (7 tests): Low, medium, high, critical attack scenarios, contradictory signal handling, missing signal reweighting, strict determinism.
- `BoundaryAndEdgeCaseTest` (5 tests): NaN, Infinity, negative values, out-of-bounds inputs, overflow attacks, CRLF log injection defense.
- `SecurityAndPrivacyTest` (5 tests): E.164 phone PII masking, SHA-256 session token generation, anti-replay nonces, timestamp drift rejection.
- `FrozenContractComplianceTest` (2 tests): Strict JSON schema validation against `risk_contract.schema.json`, integer 0-100 verification.

---

## 7. Security Assumptions
1. **Zero-Trust Network Perimeter**: Telemetry payload from Person 3 may traverse untrusted internal networks; inputs are defensively validated against NaN, negative probabilities, and out-of-bounds values.
2. **Anti-Replay Mechanism**: Nonces are cached in an LRU memory registry with a 5-minute sliding window (`MAX_TIMESTAMP_DRIFT`). Requests with drifted timestamps or duplicate nonces are flagged.
3. **Log Injection Defense**: String fields (e.g. `session_id`, `caller_id`) are stripped of `\r`, `\n`, `\t` and length-capped before logging.

---

## 8. Privacy & Data Retention Assumptions (DPDP Act / GDPR)
1. **Zero Raw Audio Retention**: The Risk Engine **never** receives, stores, or caches raw audio bytes or speech waveforms. It operates strictly on extracted mathematical features and probabilities.
2. **PII Masking**: Caller phone numbers or identity handles are masked in SOC logs (`+91-******3210`).
3. **Irreversible Pseudonymization**: A one-way SHA-256 cryptographic session hash is derived with internal salt to identify audit sessions without retaining plain personal identifiers.

---

## 9. Limitations & Edge Cases Handled
1. **Unenrolled Speaker Profiles**: When speaker verification is `UNAVAILABLE`, the engine does not guess or generate arbitrary similarity numbers. It rebalances weights to the 4 remaining signals ($w_{\text{df}}=0.50$, $w_{\text{ac}}=0.18$, $w_{\text{pr}}=0.17$, $w_{\text{ctx}}=0.15$).
2. **Contradictory Telemetry (Targeted Clones)**: If a deepfake is an exact vocal match to an executive's enrolled voice, treating `MATCH` as a pure discount could allow an attack through. The engine detects this contradiction and outputs a specific forensic alert (`"SECURITY ALERT: Speaker matches enrolled profile BUT audio exhibits synthetic neural vocoder artifacts"`).
3. **Floating Point Rounding**: All scores strictly scale to integer `0 - 100`. Double precision numbers are rounded using half-up integer rounding.

---

## 10. Contract Changes & Versioning
- **Contract Version**: `v1.2.0-composite-risk`
- **Frozen Path**: `integration/contracts/risk_contract.json` & `risk_contract.schema.json`
- **Rule of Evolution**: Any prospective change to field names or score ranges requires joint sign-off between Person 4 and Person 3. No field may be deprecated without backward-compatible defaults.

# VoiceShield — Phase 6C-4 Controlled Production Threshold Activation Document

**Project**: VoiceShield (SIH-2K26 Problem Statement 26104)  
**Phase**: Phase 6C-4 — Controlled Production Threshold Activation  
**Activation Date**: 2026-09-07  
**Status**: `PHASE 6C-4 — VERIFIED / THRESHOLD ACTIVATED`

---

## 1. Executive Summary & Explicit Authorization

Phase 6C-4 marks the controlled activation of the production speaker-verification operating threshold ($\theta_{\text{prod}} = 0.4000$) within the VoiceShield system.

Following independent mathematical and trial-construction verification in Phase 6C-3, explicit authorization was granted to configure $\theta_{\text{prod}} = 0.4000$ across application configuration files (`application.yml` and `application.properties`).

---

## 2. Threshold Provenance & Calibration Summary

- **Target Model**: SpeechBrain ECAPA-TDNN (`speechbrain/spkrec-ecapa-voxceleb`)
- **Embedding Dimension**: 192-dimensional Float32 vector
- **Cosine Calculation**: Java 64-bit double precision (`VectorUtils.cosineSimilarity`)
- **Evaluation Dataset**: LibriSpeech `test-clean` (OpenSLR 12, CC BY 4.0)
- **Speaker Partitioning**:
  - **Calibration Set**: 24 speakers, 720 trials (360 genuine, 360 impostor)
  - **Held-Out Set**: 16 speakers, 480 trials (240 genuine, 240 impostor)
- **Held-Out Confusion Matrix** ($\theta_{\text{prod}} = 0.4000$):
  - True Positives (TP): `228`
  - True Negatives (TN): `240`
  - False Positives (FP): `0`
  - False Negatives (FN): `12`
- **Held-Out Observed Performance**:
  - **Observed False Acceptance Rate (FAR)**: `0/240` (`0.00%`)
  - **False Rejection Rate (FRR)**: `5.00%` (`0.0500`)
  - **Accuracy**: `97.50%`
  - **Precision**: `100.00%`
  - **Recall**: `95.00%`
  - **F1 Score**: `0.9744`
  - **ROC Area Under Curve (AUC)**: `0.9955`
  - **Held-Out Equal Error Rate (EER)**: `2.08%` at threshold $\theta = 0.3000$
- **Risk Engine Weight Invariant**: `speakerMismatchWeight = 0.15` (`w_spk = 0.15`)

---

## 3. Configuration & Implementation Architecture

### 3.1 Externalized Configuration
The production threshold is configured via Spring Boot configuration properties:

```yaml
voiceshield:
  speaker-verification:
    threshold: ${VOICESHIELD_SPEAKER_THRESHOLD:0.4000}
```

```properties
voiceshield.speaker-verification.threshold=${VOICESHIELD_SPEAKER_THRESHOLD:0.4000}
```

### 3.2 Verification Decision Control Flow

```text
               Audio Input (WAV/FLAC)
                         ↓
               FastAPI (Python ML Service)
                         ↓
           ECAPA-TDNN Embedding Extraction
                         ↓
              192-D Float32 Vector
                         ↓
          Spring Boot SpeakerVerificationService
                         ↓
      PostgreSQL Stored Reference Embedding Profile
                         ↓
       VectorUtils.cosineSimilarity (64-Bit Double)
                         ↓
            rawCosineSimilarity >= 0.4000 ?
              /                      \
            YES                      NO
             ↓                        ↓
   Status: MATCH            Status: MISMATCH
   isMatch = true           isMatch = false
   threshold = 0.4000       threshold = 0.4000
              \                      /
               \                    /
                 Risk Engine Scoring (w_spk = 0.15)
```

### 3.3 Strict Invariants & Rejection Rules
1. **Zero / Near-Zero Norm Vectors**: $\|u\|_2 < 10^{-12}$ throws `IllegalArgumentException` and returns `SpeakerVerificationStatus.UNAVAILABLE` (`similarityScore = null`).
2. **Invalid Dimensions**: Vectors not matching exactly 192 dimensions are rejected (`UNAVAILABLE`).
3. **NaN / Infinity Values**: Input containing `NaN` or `infinity` is rejected (`UNAVAILABLE`).
4. **Unconfigured Fallback Guard**: If `voiceshield.speaker-verification.threshold` is explicitly unconfigured (`null`), `SpeakerVerificationService` returns `UNAVAILABLE` (`threshold = null`). It **cannot** silently fall back to an arbitrary numeric threshold value.

---

## 4. Statistical Limitations & Scope Bounds

> [!IMPORTANT]
> **STATISTICAL AND GENERALIZATION BOUNDS**:
> 1. The observed **0/240 false acceptances** on the held-out set does **NOT** prove that real-world production FAR is $0.00\%$ or that the system guarantees zero false acceptances.
> 2. Evaluation was conducted exclusively on **LibriSpeech test-clean** (high-SNR read audiobooks).
> 3. Performance under real-world telephone codecs (G.711 / AMR), room reverberation, acoustic background noise, or multi-speaker overlap remains **unverified**.

---

## 5. Security & Credentials Verification Audit

- **No Passwords or Credentials Injected**: PostgreSQL passwords, API keys, and JWT secrets remain strictly externalized via environment variables.
- **Privacy Enforcement**: Full speaker vector float arrays and raw audio bytes are omitted from log messages.

---

## 6. Complete Test Suite & Verification Results

All unit, integration, and end-to-end test suites passed 100%:

* **Python ML & Evaluator Suite** (`pytest`): **30 / 30 passed**
* **Java Backend Service Suite** (`mvn test` in `backend`): **27 / 27 passed**
* **Java Risk Engine Core Suite** (`mvn test` in `risk_engine`): **34 / 34 passed**
* **Total Pass Rate**: **91 / 91 passed (100% pass rate)**

---

## 7. Final Status

> **PHASE 6C-4 — VERIFIED / THRESHOLD ACTIVATED**

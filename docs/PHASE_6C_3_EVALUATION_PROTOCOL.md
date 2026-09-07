# VoiceShield — Phase 6C-3 ECAPA-TDNN Evaluation & Calibration Protocol

**Project**: VoiceShield (SIH-2K26 Problem Statement 26104)  
**Phase**: Phase 6C-3 — Empirical Evaluation & Threshold Calibration Protocol  
**Status**: `PROTOCOL SPECIFICATION & TEST FRAMEWORK READY`

---

## 1. Executive Summary

This document specifies the official, reproducible empirical evaluation protocol for the ECAPA-TDNN speaker verification system integrated into VoiceShield during Phase 6C-2.

The objective of Phase 6C-3 is to provide a rigorous, mathematical framework for evaluating genuine vs. impostor trial score distributions and establishing an evidence-based operational threshold.

---

## 2. Architectural Invariants Preserved from Phase 6C-2

1. **Model Architecture**: Pre-trained SpeechBrain ECAPA-TDNN (`speechbrain/spkrec-ecapa-voxceleb`, commit `0f99f2d0ebe89ac095bcc5903c4dd8f72b367286`) producing 192-dimensional Float32 embeddings.
2. **Audio Preprocessing**: In-memory decoding to Float32, multi-channel to mono conversion (`np.mean`), silence/NaN/Inf validation, and resampling to 16 kHz.
3. **Operational Audio Duration Policy**: Minimum audio length of 1.00s (`ECAPA_MIN_DURATION_SEC = 1.0`).
4. **Vector Math & Precision**: Production-equivalent 64-bit double precision Cosine Similarity:
   $$\text{cosine\_similarity}(u, v) = \frac{\sum_{i=1}^{192} u_i \cdot v_i}{\sqrt{\sum_{i=1}^{192} u_i^2} \cdot \sqrt{\sum_{i=1}^{192} v_i^2}}$$
5. **Zero-Norm Defense**: Epsilon validity check $\|u\|_2 < 10^{-12}$ or $\|v\|_2 < 10^{-12}$ throws exception yielding `status = UNAVAILABLE` and `similarityScore = null`. No artificial score (e.g. `0.0`) is manufactured.
6. **Production State**: In Phase 6C-2/6C-3 evaluation protocol, `threshold` remains `null` (unconfigured) until empirical calibration is authorized and performed on an approved evaluation dataset.

---

## 3. Dataset Authorization Protocol

### Critical Rule
No external speaker dataset (e.g. VoxCeleb, LibriSpeech, VCTK, VoxCeleb1-test) may be downloaded or imported automatically.

Before executing calibration, the following metadata must be authorized and recorded:
- **Dataset Name**: (e.g., VoxCeleb1-test, LibriSpeech-test-clean, or custom benchmark)
- **Source URL / Provenance**: Official dataset repository URI
- **License / Terms**: Non-commercial / academic / public domain verification
- **Speaker Count ($N$)**: Total unique enrolled speakers
- **Recording Count ($M$)**: Total audio utterances
- **Train / Dev / Test Partition**: Strict separation ensuring zero speaker leakage between development tuning and held-out evaluation testing.

If no authorized evaluation dataset is present:
> **Phase 6C-3 requires an authorized evaluation dataset before empirical calibration can proceed.**

---

## 4. Trial Construction Methodology

The protocol generates two disjoint trial classes:

### 4.1 Genuine Trials (Same-Speaker $A \rightarrow A$)
- Enrolled reference recording: Speaker $A$ (Utterance 1)
- Query verification recording: Speaker $A$ (Utterance 2)
- Expected Ground Truth Label: `GENUINE` (Target)

### 4.2 Impostor Trials (Cross-Speaker $A \rightarrow B$)
- Enrolled reference recording: Speaker $A$ (Utterance 1)
- Query verification recording: Speaker $B$ (Utterance 1) ($A \neq B$)
- Expected Ground Truth Label: `IMPOSTOR` (Non-Target)

---

## 5. Statistical & Performance Metrics

For every candidate threshold $\theta \in [-0.99, +0.99]$ in increments of $0.01$:

1. **Confusion Matrix**:
   - True Positives ($\text{TP}$): Genuine trials with score $s \ge \theta$
   - False Negatives ($\text{FN}$): Genuine trials with score $s < \theta$
   - False Positives ($\text{FP}$): Impostor trials with score $s \ge \theta$
   - True Negatives ($\text{TN}$): Impostor trials with score $s < \theta$

2. **Error Rates**:
   - False Acceptance Rate ($\text{FAR}$): $\text{FAR}(\theta) = \frac{\text{FP}}{\text{FP} + \text{TN}}$
   - False Rejection Rate ($\text{FRR}$): $\text{FRR}(\theta) = \frac{\text{FN}}{\text{FN} + \text{TP}}$

3. **Equal Error Rate (EER)**:
   - Point where $\text{FAR}(\theta_{\text{EER}}) \approx \text{FRR}(\theta_{\text{EER}})$.

4. **Area Under Curve (AUC)**:
   - Trapezoidal integration of Receiver Operating Characteristic ($\text{TPR}(\theta) = 1 - \text{FRR}(\theta)$ vs. $\text{FAR}(\theta)$).

---

## 6. Security-Driven Operating Point Selection

The selection of the final production threshold $\theta_{\text{prod}}$ must balance security objectives:
- **Security Priority (Low FAR)**: High-security transactions prefer $\theta > \theta_{\text{EER}}$ to minimize fraudulent impostor acceptances ($\text{FAR} \le 0.1\%$).
- **Usability Priority (Low FRR)**: Frictionless authentication prefers $\theta < \theta_{\text{EER}}$ to avoid rejecting genuine users ($\text{FRR} \le 1.0\%$).

Threshold selection must be configuration-driven in `application.properties` (e.g. `voiceshield.speaker.threshold=0.65`) and must NEVER be hardcoded into neural or service logic.

---

## 7. Execution Architecture

The evaluation protocol runner script is located at:
`evaluation/scripts/evaluate_ecapa.py`

Unit tests verifying the evaluation protocol runner are located at:
`evaluation/tests/test_ecapa_evaluator.py`

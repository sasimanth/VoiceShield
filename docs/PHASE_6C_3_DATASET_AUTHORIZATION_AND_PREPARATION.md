# VoiceShield — Phase 6C-3 Dataset Authorization & Preparation Report

**Project**: VoiceShield (SIH-2K26 Problem Statement 26104)  
**Phase**: Phase 6C-3 — Dataset Authorization & Preparation  
**Status**: `DATASET PREPARATION FRAMEWORK READY / WAITING FOR DATASET SELECTION & AUTHORIZATION`

---

## 1. Dataset Candidate Options & Licensing Audit

### Candidate Option A: VoxCeleb1 (Test & Verification Subset)
1. **Official Name**: VoxCeleb1 Speaker Identification & Verification Corpus
2. **Official Source**: University of Oxford (VGG) — `https://www.robots.ox.ac.uk/~vgg/data/voxceleb/`
3. **Official Documentation**: Nagrani et al., "VoxCeleb: a large-scale speaker identification dataset", INTERSPEECH 2017
4. **License / Terms of Use**: Creative Commons Attribution 4.0 International (CC BY 4.0)
5. **Intended Usage Restrictions**: Non-commercial research, academic, and technology evaluation
6. **Academic/Research Permitted**: YES
7. **Project / SIH Evaluation Permitted**: YES
8. **Redistribution Permitted**: Audio files subject to YouTube source terms; trial pair manifests & extracted derivative embeddings permitted under CC BY 4.0
9. **Registration Required**: Formal registration form on VGG Oxford website
10. **Storage Location**: `evaluation/data/speaker_verification/voxceleb1/` (strictly excluded from Git)

### Candidate Option B: LibriSpeech (test-clean & dev-clean Speaker Verification Subset)
1. **Official Name**: LibriSpeech ASR / Speaker Verification Corpus
2. **Official Source**: OpenSLR — `http://www.openslr.org/12`
3. **Official Documentation**: Panayotov et al., "LibriSpeech: an ASR corpus based on public domain audio books", ICASSP 2015
4. **License / Terms of Use**: Creative Commons Attribution 4.0 International (CC BY 4.0)
5. **Intended Usage Restrictions**: Unrestricted Open Access (Research & Commercial)
6. **Academic/Research Permitted**: YES
7. **Project / SIH Evaluation Permitted**: YES
8. **Redistribution Permitted**: YES
9. **Registration Required**: NO (Direct Public Download via OpenSLR)
10. **Storage Location**: `evaluation/data/speaker_verification/librispeech/` (strictly excluded from Git)

---

## 2. Dataset Structure & Partition Design

The preparation script (`evaluation/scripts/prepare_speaker_dataset.py`) enforces strict speaker-independent partitioning:

### 2.1 Calibration / Development Split (60% of Speakers)
- Used for: Threshold sweep ($\theta \in [-0.99, +0.99]$), EER analysis, ROC curve calculation, and candidate threshold selection.

### 2.2 Held-Out Evaluation Split (40% of Speakers)
- Used for: Unbiased final performance measurement after threshold candidate selection.
- **Zero Speaker Leakage Control**: `set(dev_speakers).intersection(set(heldout_speakers)) == 0`. Every speaker ID exists strictly in either the calibration split or held-out split.

---

## 3. Trial-Generation Design

Trial manifests are generated deterministically:
- **Genuine Trials ($A_1 \leftrightarrow A_2$)**: Two distinct recordings from the same speaker. Ground Truth Label: `GENUINE` (`is_target = True`).
- **Impostor Trials ($A_1 \leftrightarrow B_1$)**: Recordings from distinct speakers ($A \neq B$). Ground Truth Label: `IMPOSTOR` (`is_target = False`).

---

## 4. Git Exclusion & Security Control

Dataset audio files and trial manifests stored in `evaluation/data/` are strictly excluded from Git tracking in `.gitignore`:
```gitignore
evaluation/data/*
!evaluation/data/.gitkeep
!evaluation/data/**/.gitkeep
```
Zero credentials, access tokens, or raw audio files are stored in Docker images, logs, or committed source code.

---

## 5. System & Risk Engine Invariants Preserved

- **Risk Engine Decision Weights**: Preserved `w_spk = 0.15` without alteration.
- **Production Threshold**: `Double threshold = null` (unconfigured) until empirical calibration is executed on the authorized dataset.
- **Pipeline Consistency**: Uses identical production ECAPA 192-D embedding extraction and 64-bit double precision Java cosine similarity math.

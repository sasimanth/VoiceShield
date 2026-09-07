# VoiceShield — AASIST Model Evaluation & Calibration Framework

This directory contains the reproducible scientific evaluation, latency measurement, and threshold calibration framework for the VoiceShield AASIST deepfake detection pipeline.

---

## 1. Directory Structure

```text
evaluation/
├── README.md                 # Documentation & methodology guide
├── requirements.txt          # Python dependencies for evaluation
├── data/                     # Controlled evaluation dataset root (gitignored)
│   ├── bonafide/             # Genuine human speech utterances (.wav, .flac, .mp3)
│   └── spoof/                # Synthetic speech / deepfakes / voice clones
├── results/                  # Generated evaluation outputs (metrics, CSVs)
│   ├── metrics.json          # Overall evaluation summary & metadata
│   ├── predictions.csv       # Per-sample prediction & latency log
│   └── thresholds.csv        # Full 101-point threshold sweep table (0.00 -> 1.00)
├── scripts/
│   └── evaluate_aasist.py    # Main evaluation script
└── tests/
    └── test_evaluator.py     # Automated unit tests for evaluation framework
```

---

## 2. Preparing Evaluation Data

Place evaluation audio samples into the following directory structure:

- **Genuine Human Speech**: Place audio files under `evaluation/data/bonafide/`.
  - Subdirectories (e.g. `evaluation/data/bonafide/clean/`, `evaluation/data/bonafide/noisy/`) are automatically detected and recorded as robustness categories.
- **Deepfake / AI Synthetic Voice**: Place audio files under `evaluation/data/spoof/`.
  - Subdirectories (e.g. `evaluation/data/spoof/elevenlabs/`, `evaluation/data/spoof/tortoise/`, `evaluation/data/spoof/rvc/`) are automatically detected and recorded as attack type categories.

### Requirements:
- Supported formats: `.wav`, `.flac`, `.mp3`, `.ogg`, `.m4a`.
- Preprocessing: Standardized automatically by the pipeline to 16,000 Hz mono PCM float32 with 64,600 samples (~4.0375 seconds).

---

## 3. Running Evaluation

Run the evaluator via command-line:

```bash
python evaluation/scripts/evaluate_aasist.py --data-dir evaluation/data --output-dir evaluation/results
```

### Optional Arguments:
- `--data-dir`: Path to evaluation data root (default: `evaluation/data`).
- `--output-dir`: Path to directory where results are written (default: `evaluation/results`).
- `--checkpoint-path`: Path to AASIST model checkpoint (default: `ml/deepfake_detector/checkpoints/AASIST.pth`).
- `--threshold-step`: Step size for threshold sweep (default: `0.01`).

---

## 4. Metric Definitions

- **ROC-AUC**: Area Under the Receiver Operating Characteristic Curve ($[0.0, 1.0]$).
- **Equal Error Rate (EER)**: The threshold where False Alarm Rate ($\text{FAR}$) equals False Rejection Rate ($\text{FRR}$).
- **False Alarm Rate (FAR / FPR)**: Fraction of genuine human utterances incorrectly flagged as deepfakes ($FAR = \frac{FP}{FP + TN}$).
- **False Rejection Rate (FRR / FNR)**: Fraction of deepfake utterances incorrectly accepted as genuine human speech ($FRR = \frac{FN}{FN + TP}$).
- **Accuracy**: Overall fraction of correct predictions ($\frac{TP + TN}{TP + TN + FP + FN}$).
- **Precision**: $\frac{TP}{TP + FP}$.
- **Recall**: $\frac{TP}{TP + FN}$.
- **F1 Score**: Harmonic mean of Precision and Recall ($\frac{2 \cdot \text{Precision} \cdot \text{Recall}}{\text{Precision} + \text{Recall}}$).

---

## 5. Threshold Selection Methodology

The framework performs a 101-point threshold sweep ($0.00 \to 1.00$ in steps of $0.01$) and identifies:
1. **EER Operating Threshold**: Threshold minimizing $|FAR - FRR|$. Recommended for balanced threat models.
2. **Best F1 Threshold**: Threshold maximizing F1 score. Recommended for precision/recall balance.
3. **Target FAR Thresholds**: Thresholds enforcing $FAR \le 1\%$ or $FAR \le 5\%$. Recommended for security-critical deployments requiring minimal false alarms on legitimate users.

---

## 6. Output Files

- `evaluation/results/metrics.json`: Full JSON summary including environmental reproducibility metadata (Python version, PyTorch version, checkpoint SHA-256), overall metrics, recommended thresholds, Brier calibration score, latency statistics, and robustness subset breakdown.
- `evaluation/results/predictions.csv`: Per-sample prediction log recording `filename`, `relative_path`, `category`, `ground_truth`, `deepfake_probability`, `bonafide_probability`, `inference_time_ms`, `preprocess_time_ms`, and `status`.
- `evaluation/results/thresholds.csv`: Complete metrics table for all 101 threshold sweep points.

---

## 7. Privacy & Security Rules

- Evaluation logs and output CSV/JSON files **never store raw audio bytes or waveforms**.
- Audio files in `evaluation/data/` are gitignored to prevent committing proprietary/sensitive audio assets into Git repositories.
- Zero passwords, credentials, or secret keys are used or stored.

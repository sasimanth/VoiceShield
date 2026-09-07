"""
Scientific Evaluation & Calibration Framework for VoiceShield AASIST Deepfake Detector.

Primary Objective:
Evaluate the integrated official AASIST model on a controlled audio dataset,
measure inference latency, calculate ROC-AUC, EER, FAR, FRR, Accuracy, Precision, Recall, F1,
perform threshold sweeps, and determine the optimal operating threshold for VoiceShield.

Usage:
python evaluation/scripts/evaluate_aasist.py --data-dir evaluation/data --output-dir evaluation/results
"""

import os
import sys
import time
import json
import csv
import glob
import math
import hashlib
import argparse
from typing import Dict, Any, List, Tuple, Optional

import numpy as np
import torch
import soundfile as sf
import scipy
import scipy.signal as signal

from ml.deepfake_detector.pipeline import DeepfakeDetector
from ml_service.main import decode_and_preprocess_audio


def calculate_sha256(filepath: str) -> str:
    """Calculates SHA-256 hash of a file."""
    if not os.path.exists(filepath):
        return "FILE_NOT_FOUND"
    hasher = hashlib.sha256()
    with open(filepath, "rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


def compute_binary_metrics(y_true: np.ndarray, y_score: np.ndarray, threshold: float) -> Dict[str, float]:
    """
    Computes binary classification metrics at a given threshold.
    Positive Class (1) = SPOOF (Deepfake)
    Negative Class (0) = BONAFIDE (Human)
    """
    y_pred = (y_score >= threshold).astype(int)

    tp = int(np.sum((y_pred == 1) & (y_true == 1)))
    tn = int(np.sum((y_pred == 0) & (y_true == 0)))
    fp = int(np.sum((y_pred == 1) & (y_true == 0)))
    fn = int(np.sum((y_pred == 0) & (y_true == 1)))

    n_bonafide = tp + fn if (tp + fn) > 0 else 0
    num_neg = fp + tn if (fp + tn) > 0 else 0
    num_pos = tp + fn if (tp + fn) > 0 else 0

    far = float(fp / num_neg) if num_neg > 0 else 0.0  # False Alarm Rate (Bonafide flagged as Spoof)
    frr = float(fn / num_pos) if num_pos > 0 else 0.0  # False Reject Rate (Spoof accepted as Bonafide)

    acc = float((tp + tn) / len(y_true)) if len(y_true) > 0 else 0.0
    prec = float(tp / (tp + fp)) if (tp + fp) > 0 else 0.0
    rec = float(tp / (tp + fn)) if (tp + fn) > 0 else 0.0
    f1 = float(2 * prec * rec / (prec + rec)) if (prec + rec) > 0 else 0.0

    return {
        "threshold": round(threshold, 4),
        "tp": tp,
        "tn": tn,
        "fp": fp,
        "fn": fn,
        "far": round(far, 6),
        "frr": round(frr, 6),
        "accuracy": round(acc, 6),
        "precision": round(prec, 6),
        "recall": round(rec, 6),
        "f1_score": round(f1, 6)
    }


def compute_roc_auc(y_true: np.ndarray, y_score: np.ndarray) -> float:
    """Calculates ROC-AUC using trapezoidal rule over sorted prediction thresholds."""
    if len(np.unique(y_true)) < 2:
        return 0.5  # Cannot compute ROC-AUC with only one class present

    desc_indices = np.argsort(-y_score)
    y_true_sorted = y_true[desc_indices]
    y_score_sorted = y_score[desc_indices]

    n_pos = np.sum(y_true == 1)
    n_neg = np.sum(y_true == 0)

    if n_pos == 0 or n_neg == 0:
        return 0.5

    tps = np.cumsum(y_true_sorted == 1)
    fps = np.cumsum(y_true_sorted == 0)

    tpr = tps / n_pos
    fpr = fps / n_neg

    tpr = np.concatenate([[0.0], tpr, [1.0]])
    fpr = np.concatenate([[0.0], fpr, [1.0]])

    trapz_fn = getattr(np, "trapezoid", getattr(np, "trapz", None))
    auc = float(trapz_fn(tpr, fpr))
    return round(auc, 6)


def compute_brier_score(y_true: np.ndarray, y_score: np.ndarray) -> float:
    """Computes Brier calibration score (mean squared error of probability predictions)."""
    if len(y_true) == 0:
        return 0.0
    return float(np.mean((y_score - y_true) ** 2))


def evaluate(data_dir: str, output_dir: str, checkpoint_path: str, threshold_step: float = 0.01) -> Dict[str, Any]:
    """Runs full evaluation pipeline over data_dir/bonafide and data_dir/spoof."""
    start_time_iso = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    os.makedirs(output_dir, exist_ok=True)

    ckpt_hash = calculate_sha256(checkpoint_path)
    ckpt_filename = os.path.basename(checkpoint_path)

    detector = DeepfakeDetector(checkpoint_path=checkpoint_path)

    # Audio extensions to search for
    valid_exts = ("*.wav", "*.flac", "*.mp3", "*.ogg", "*.m4a")

    bonafide_dir = os.path.join(data_dir, "bonafide")
    spoof_dir = os.path.join(data_dir, "spoof")

    audio_entries = []

    # Discover Bonafide audio files (ground_truth = 0)
    if os.path.exists(bonafide_dir):
        for root, _, files in os.walk(bonafide_dir):
            for file in files:
                if file.lower().endswith((".wav", ".flac", ".mp3", ".ogg", ".m4a")):
                    rel_category = os.path.relpath(root, bonafide_dir)
                    category_name = "bonafide_clean" if rel_category == "." else f"bonafide_{rel_category}"
                    audio_entries.append({
                        "filepath": os.path.join(root, file),
                        "relative_path": os.path.relpath(os.path.join(root, file), data_dir),
                        "filename": file,
                        "ground_truth": 0,
                        "category": category_name
                    })

    # Discover Spoof audio files (ground_truth = 1)
    if os.path.exists(spoof_dir):
        for root, _, files in os.walk(spoof_dir):
            for file in files:
                if file.lower().endswith((".wav", ".flac", ".mp3", ".ogg", ".m4a")):
                    rel_category = os.path.relpath(root, spoof_dir)
                    category_name = "spoof_gen" if rel_category == "." else f"spoof_{rel_category}"
                    audio_entries.append({
                        "filepath": os.path.join(root, file),
                        "relative_path": os.path.relpath(os.path.join(root, file), data_dir),
                        "filename": file,
                        "ground_truth": 1,
                        "category": category_name
                    })

    total_samples = len(audio_entries)
    bonafide_count = sum(1 for e in audio_entries if e["ground_truth"] == 0)
    spoof_count = sum(1 for e in audio_entries if e["ground_truth"] == 1)

    if total_samples == 0:
        print("[WARN] No evaluation audio files found in data directory. Status: DATASET_NOT_AVAILABLE")
        res_summary = {
            "status": "DATASET_NOT_AVAILABLE",
            "evaluation_timestamp": start_time_iso,
            "reproducibility": {
                "python_version": sys.version.split()[0],
                "pytorch_version": torch.__version__,
                "numpy_version": np.__version__,
                "scipy_version": scipy.__version__,
                "checkpoint_filename": ckpt_filename,
                "checkpoint_sha256": ckpt_hash,
                "device": str(detector.device)
            },
            "dataset": {
                "data_dir": data_dir,
                "total_samples": 0,
                "bonafide_count": 0,
                "spoof_count": 0
            },
            "metrics": None,
            "latency": None,
            "calibration": {
                "status": "CALIBRATION_NOT_ESTABLISHED",
                "brier_score": None
            }
        }
        with open(os.path.join(output_dir, "metrics.json"), "w") as f:
            json.dump(res_summary, f, indent=2)
        return res_summary

    print(f"[INFO] Discovered {total_samples} samples ({bonafide_count} Bonafide, {spoof_count} Spoof). Running AASIST evaluation...")

    results = []
    model_latencies = []
    total_latencies = []

    for entry in audio_entries:
        fpath = entry["filepath"]
        t_start = time.perf_counter()

        try:
            with open(fpath, "rb") as f:
                raw_bytes = f.read()

            t_proc_start = time.perf_counter()
            audio_data = decode_and_preprocess_audio(raw_bytes, target_sr=16000)
            preprocess_time_ms = (time.perf_counter() - t_proc_start) * 1000.0

            t_infer_start = time.perf_counter()
            prediction = detector.predict(audio_data)
            inference_time_ms = (time.perf_counter() - t_infer_start) * 1000.0

            total_time_ms = (time.perf_counter() - t_start) * 1000.0

            prob = float(prediction.get("deepfake_probability", 0.0))
            status_str = str(prediction.get("status", "UNKNOWN"))

            model_latencies.append(inference_time_ms)
            total_latencies.append(total_time_ms)

            results.append({
                "filename": entry["filename"],
                "relative_path": entry["relative_path"],
                "category": entry["category"],
                "ground_truth": entry["ground_truth"],
                "deepfake_probability": prob,
                "bonafide_probability": float(prediction.get("bonafide_probability", 1.0 - prob)),
                "inference_time_ms": round(inference_time_ms, 2),
                "preprocess_time_ms": round(preprocess_time_ms, 2),
                "total_time_ms": round(total_time_ms, 2),
                "status": status_str
            })

        except Exception as e:
            print(f"[WARN] Error evaluating file '{fpath}': {e}")
            results.append({
                "filename": entry["filename"],
                "relative_path": entry["relative_path"],
                "category": entry["category"],
                "ground_truth": entry["ground_truth"],
                "deepfake_probability": 0.0,
                "bonafide_probability": 0.0,
                "inference_time_ms": 0.0,
                "preprocess_time_ms": 0.0,
                "total_time_ms": 0.0,
                "status": f"ERROR: {str(e)}"
            })

    # Write predictions.csv
    csv_path = os.path.join(output_dir, "predictions.csv")
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=[
            "filename", "relative_path", "category", "ground_truth",
            "deepfake_probability", "bonafide_probability",
            "inference_time_ms", "preprocess_time_ms", "total_time_ms", "status"
        ])
        writer.writeheader()
        writer.writerows(results)

    # Extract arrays for valid predictions
    valid_results = [r for r in results if r["status"] == "SUCCESS"]
    y_true = np.array([r["ground_truth"] for r in valid_results], dtype=int)
    y_score = np.array([r["deepfake_probability"] for r in valid_results], dtype=float)

    has_both_classes = (len(np.unique(y_true)) >= 2)

    # Threshold sweep from 0.00 to 1.00
    threshold_steps = np.arange(0.0, 1.0001, threshold_step)
    threshold_results = []

    best_eer_thresh = 0.50
    min_eer_diff = 1.0
    eer_value = None

    best_f1_thresh = 0.50
    max_f1 = -1.0

    target_far_1pct_thresh = None
    target_far_5pct_thresh = None

    for th in threshold_steps:
        res_m = compute_binary_metrics(y_true, y_score, float(th))
        threshold_results.append(res_m)

        far = res_m["far"]
        frr = res_m["frr"]
        f1 = res_m["f1_score"]

        # EER threshold identification (where |FAR - FRR| is minimal)
        diff = abs(far - frr)
        if diff < min_eer_diff:
            min_eer_diff = diff
            best_eer_thresh = float(th)
            eer_value = round((far + frr) / 2.0, 6)

        # Best F1 identification
        if f1 > max_f1:
            max_f1 = f1
            best_f1_thresh = float(th)

        # Target FAR identification
        if target_far_1pct_thresh is None and far <= 0.01:
            target_far_1pct_thresh = float(th)
        if target_far_5pct_thresh is None and far <= 0.05:
            target_far_5pct_thresh = float(th)

    # Write thresholds.csv
    thresh_csv_path = os.path.join(output_dir, "thresholds.csv")
    with open(thresh_csv_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=[
            "threshold", "tp", "tn", "fp", "fn", "far", "frr",
            "accuracy", "precision", "recall", "f1_score"
        ])
        writer.writeheader()
        writer.writerows(threshold_results)

    # Calculate overall metrics at default threshold (0.50) and recommended EER threshold
    metrics_at_050 = compute_binary_metrics(y_true, y_score, 0.50)
    metrics_at_eer = compute_binary_metrics(y_true, y_score, best_eer_thresh)
    roc_auc = compute_roc_auc(y_true, y_score) if has_both_classes else 0.5
    brier_score = compute_brier_score(y_true, y_score)

    # Robustness subsets per category
    subsets_metrics = {}
    categories = sorted(list(set(r["category"] for r in valid_results)))
    for cat in categories:
        cat_results = [r for r in valid_results if r["category"] == cat]
        cat_true = np.array([r["ground_truth"] for r in cat_results])
        cat_score = np.array([r["deepfake_probability"] for r in cat_results])
        subsets_metrics[cat] = {
            "sample_count": len(cat_results),
            "metrics_at_050": compute_binary_metrics(cat_true, cat_score, 0.50),
            "mean_score": round(float(np.mean(cat_score)), 4) if len(cat_score) > 0 else 0.0
        }

    # Latency statistics
    if len(model_latencies) > 0:
        latency_stats = {
            "sample_count": len(model_latencies),
            "inference_mean_ms": round(float(np.mean(model_latencies)), 2),
            "inference_median_ms": round(float(np.median(model_latencies)), 2),
            "inference_p95_ms": round(float(np.percentile(model_latencies, 95)), 2),
            "inference_p99_ms": round(float(np.percentile(model_latencies, 99)), 2),
            "inference_min_ms": round(float(np.min(model_latencies)), 2),
            "inference_max_ms": round(float(np.max(model_latencies)), 2),
            "total_latency_mean_ms": round(float(np.mean(total_latencies)), 2)
        }
    else:
        latency_stats = None

    eval_status = "EVALUATION_COMPLETE" if has_both_classes else "SINGLE_CLASS_EVALUATION"

    res_summary = {
        "status": eval_status,
        "evaluation_timestamp": start_time_iso,
        "reproducibility": {
            "python_version": sys.version.split()[0],
            "pytorch_version": torch.__version__,
            "numpy_version": np.__version__,
            "scipy_version": scipy.__version__,
            "checkpoint_filename": ckpt_filename,
            "checkpoint_sha256": ckpt_hash,
            "device": str(detector.device)
        },
        "dataset": {
            "data_dir": data_dir,
            "total_samples": total_samples,
            "bonafide_count": bonafide_count,
            "spoof_count": spoof_count,
            "has_both_classes": has_both_classes
        },
        "overall_metrics": {
            "roc_auc": roc_auc,
            "eer": eer_value if has_both_classes else "SINGLE_CLASS_UNAVAILABLE",
            "at_default_threshold_050": metrics_at_050,
            "at_eer_threshold": metrics_at_eer if has_both_classes else None
        },
        "recommended_thresholds": {
            "default_threshold": 0.50,
            "eer_operating_threshold": best_eer_thresh if has_both_classes else 0.50,
            "best_f1_threshold": best_f1_thresh,
            "target_far_1pct_threshold": target_far_1pct_thresh,
            "target_far_5pct_threshold": target_far_5pct_thresh
        },
        "calibration": {
            "status": "ESTABLISHED" if len(valid_results) >= 10 else "CALIBRATION_NOT_ESTABLISHED",
            "brier_score": round(brier_score, 6)
        },
        "latency_stats": latency_stats,
        "subsets_metrics": subsets_metrics
    }

    # Write metrics.json
    metrics_json_path = os.path.join(output_dir, "metrics.json")
    with open(metrics_json_path, "w", encoding="utf-8") as f:
        json.dump(res_summary, f, indent=2)

    print(f"\n=======================================================")
    print(f"VOICE SHIELD AASIST EVALUATION SUMMARY ({eval_status})")
    print(f"=======================================================")
    print(f"Total Samples: {total_samples} (Bonafide: {bonafide_count}, Spoof: {spoof_count})")
    print(f"Checkpoint SHA-256: {ckpt_hash[:16]}...")
    if has_both_classes:
        print(f"ROC-AUC: {roc_auc:.4f}")
        eer_str = f"{eer_value:.4f}" if isinstance(eer_value, float) else str(eer_value)
        print(f"EER: {eer_str} (at threshold {best_eer_thresh:.2f})")
        print(f"Default (th=0.50) -> Accuracy: {metrics_at_050['accuracy']:.4f}, F1: {metrics_at_050['f1_score']:.4f}, FAR: {metrics_at_050['far']:.4f}, FRR: {metrics_at_050['frr']:.4f}")
        print(f"Recommended Threshold: {best_eer_thresh:.2f}")
    if latency_stats:
        print(f"Inference Latency -> Mean: {latency_stats['inference_mean_ms']} ms, Median: {latency_stats['inference_median_ms']} ms, P95: {latency_stats['inference_p95_ms']} ms")
    print(f"Output files saved to: {output_dir}")
    print(f"=======================================================\n")

    return res_summary


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="VoiceShield AASIST Evaluation & Calibration Script")
    parser.add_argument("--data-dir", type=str, default="evaluation/data", help="Directory containing bonafide/ and spoof/ audio samples")
    parser.add_argument("--output-dir", type=str, default="evaluation/results", help="Directory to save metrics.json, predictions.csv, thresholds.csv")
    parser.add_argument("--checkpoint-path", type=str, default=os.path.join("ml", "deepfake_detector", "checkpoints", "AASIST.pth"), help="Path to AASIST checkpoint")
    parser.add_argument("--threshold-step", type=float, default=0.01, help="Step size for threshold sweep")

    args = parser.parse_args()
    evaluate(data_dir=args.data_dir, output_dir=args.output_dir, checkpoint_path=args.checkpoint_path, threshold_step=args.threshold_step)

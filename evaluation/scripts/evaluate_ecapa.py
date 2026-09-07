import os
import sys
import json
import logging
import math
import numpy as np
from typing import List, Dict, Any, Tuple, Optional

logger = logging.getLogger("voiceshield.evaluation.ecapa")

def compute_cosine_similarity(u: np.ndarray, v: np.ndarray) -> Optional[float]:
    """
    Computes exact mathematical Cosine Similarity in 64-bit double precision,
    matching production VectorUtils.cosineSimilarity Java implementation.
    Returns None for zero-norm or invalid vectors.
    """
    if u is None or v is None:
        return None
    if u.shape != (192,) or v.shape != (192,):
        return None
    
    u64 = u.astype(np.float64)
    v64 = v.astype(np.float64)

    if np.isnan(u64).any() or np.isinf(u64).any() or np.isnan(v64).any() or np.isinf(v64).any():
        return None

    dot_product = np.dot(u64, v64)
    norm_u = np.sqrt(np.dot(u64, u64))
    norm_v = np.sqrt(np.dot(v64, v64))

    # Numerical zero-norm validity guard (norm < 1e-12)
    if norm_u < 1e-12 or norm_v < 1e-12:
        return None

    return float(dot_product / (norm_u * norm_v))


class ECAPAStatsCalculator:
    """Calculates summary distribution statistics for trial score lists."""
    @staticmethod
    def calculate_stats(scores: List[float]) -> Dict[str, float]:
        if not scores:
            return {"min": 0.0, "max": 0.0, "mean": 0.0, "median": 0.0, "std": 0.0}
        arr = np.array(scores, dtype=np.float64)
        return {
            "min": float(np.min(arr)),
            "max": float(np.max(arr)),
            "mean": float(np.mean(arr)),
            "median": float(np.median(arr)),
            "std": float(np.std(arr))
        }


class ECAPAEvaluator:
    """
    Empirical evaluator for ECAPA-TDNN speaker verification.
    Computes EER, AUC, FAR/FRR curves, and threshold analysis.
    """

    def __init__(self, genuine_scores: List[float], impostor_scores: List[float]):
        self.genuine_scores = [s for s in genuine_scores if s is not None]
        self.impostor_scores = [s for s in impostor_scores if s is not None]

    def evaluate(self) -> Dict[str, Any]:
        if not self.genuine_scores or not self.impostor_scores:
            return {
                "status": "ERROR",
                "message": "Insufficient trial scores for evaluation.",
                "genuine_count": len(self.genuine_scores),
                "impostor_count": len(self.impostor_scores)
            }

        gen_stats = ECAPAStatsCalculator.calculate_stats(self.genuine_scores)
        imp_stats = ECAPAStatsCalculator.calculate_stats(self.impostor_scores)

        # Candidate thresholds from -0.99 to +0.99 in steps of 0.01
        threshold_candidates = np.linspace(-0.99, 0.99, 199)
        threshold_results = []

        min_eer_diff = float("inf")
        best_eer = 0.0
        best_eer_threshold = 0.0

        num_gen = len(self.genuine_scores)
        num_imp = len(self.impostor_scores)

        for th in threshold_candidates:
            # Classification decision logic for evaluation
            # Score >= th -> Predicted Match (Genuine)
            # Score < th -> Predicted Mismatch (Impostor)
            tp = sum(1 for s in self.genuine_scores if s >= th)
            fn = num_gen - tp
            fp = sum(1 for s in self.impostor_scores if s >= th)
            tn = num_imp - fp

            far = fp / float(num_imp) if num_imp > 0 else 0.0
            frr = fn / float(num_gen) if num_gen > 0 else 0.0

            accuracy = (tp + tn) / float(num_gen + num_imp)
            precision = tp / float(tp + fp) if (tp + fp) > 0 else 0.0
            recall = tp / float(num_gen) if num_gen > 0 else 0.0
            f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0

            threshold_results.append({
                "threshold": float(round(th, 4)),
                "tp": tp, "tn": tn, "fp": fp, "fn": fn,
                "far": float(round(far, 4)),
                "frr": float(round(frr, 4)),
                "accuracy": float(round(accuracy, 4)),
                "precision": float(round(precision, 4)),
                "recall": float(round(recall, 4)),
                "f1": float(round(f1, 4))
            })

            eer_diff = abs(far - frr)
            if eer_diff < min_eer_diff:
                min_eer_diff = eer_diff
                best_eer = (far + frr) / 2.0
                best_eer_threshold = float(round(th, 4))

        # Compute AUC using Trapezoidal rule (NumPy 2.0+ compatible)
        sorted_res = sorted(threshold_results, key=lambda x: x["far"])
        far_vals = [r["far"] for r in sorted_res]
        tpr_vals = [1.0 - r["frr"] for r in sorted_res]
        trapz_fn = getattr(np, 'trapezoid', getattr(np, 'trapz', None))
        auc = float(trapz_fn(tpr_vals, far_vals))

        return {
            "status": "SUCCESS",
            "trial_counts": {
                "genuine_trials": num_gen,
                "impostor_trials": num_imp,
                "total_trials": num_gen + num_imp
            },
            "statistics": {
                "genuine_scores": gen_stats,
                "impostor_scores": imp_stats
            },
            "eer_analysis": {
                "eer": float(round(best_eer, 4)),
                "eer_threshold": best_eer_threshold
            },
            "roc_analysis": {
                "auc": float(round(auc, 4))
            },
            "threshold_sweep": threshold_results
        }

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    print("ECAPA-TDNN Speaker Verification Evaluation Framework initialized.")

"""
Benchmark Evaluation Suite (EER, ROC-AUC, Precision, Recall)
Owner: Person 6 (Real-Time Integration & QA Engineer)
"""

import numpy as np
from typing import Dict, Any, List
from sklearn.metrics import roc_curve, roc_auc_score, precision_recall_fscore_support, confusion_matrix


class BenchmarkSuite:
    """Calculates Equal Error Rate (EER) and forensic classification performance metrics."""

    @staticmethod
    def compute_metrics(y_true: List[int], y_scores: List[float]) -> Dict[str, Any]:
        """
        y_true: 0 for Bonafide (Human), 1 for Spoof (Deepfake Clone)
        y_scores: Continuous synthetic probability / risk score (0.0 to 1.0)
        """
        y_true_arr = np.array(y_true)
        y_scores_arr = np.array(y_scores)

        # ROC Curve & AUC
        fpr, tpr, thresholds = roc_curve(y_true_arr, y_scores_arr)
        auc_score = float(roc_auc_score(y_true_arr, y_scores_arr))

        # Equal Error Rate (EER): Point where False Acceptance Rate (FAR) == False Rejection Rate (FRR)
        fnr = 1 - tpr
        eer_idx = np.nanargmin(np.abs(fpr - fnr))
        eer = float((fpr[eer_idx] + fnr[eer_idx]) / 2.0)
        eer_threshold = float(thresholds[eer_idx])

        # Binary predictions at standard 0.50 threshold
        y_pred = (y_scores_arr >= 0.50).astype(int)
        precision, recall, f1, _ = precision_recall_fscore_support(y_true_arr, y_pred, average="binary", zero_division=0)
        cm = confusion_matrix(y_true_arr, y_pred)

        return {
            "equal_error_rate_eer": round(eer * 100, 2),  # In percentage %
            "optimal_eer_threshold": round(eer_threshold, 4),
            "roc_auc_score": round(auc_score, 4),
            "precision": round(float(precision), 4),
            "recall": round(float(recall), 4),
            "f1_score": round(float(f1), 4),
            "confusion_matrix": {
                "true_negatives_human": int(cm[0, 0]),
                "false_positives": int(cm[0, 1]),
                "false_negatives": int(cm[1, 0]),
                "true_positives_spoof": int(cm[1, 1])
            }
        }

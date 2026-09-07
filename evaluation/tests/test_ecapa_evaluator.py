import pytest
import numpy as np
from evaluation.scripts.evaluate_ecapa import compute_cosine_similarity, ECAPAEvaluator, ECAPAStatsCalculator

def test_compute_cosine_similarity_identical():
    u = np.ones(192, dtype=np.float32)
    sim = compute_cosine_similarity(u, u)
    assert sim is not None
    assert abs(sim - 1.0) < 1e-5

def test_compute_cosine_similarity_opposite():
    u = np.ones(192, dtype=np.float32)
    v = -np.ones(192, dtype=np.float32)
    sim = compute_cosine_similarity(u, v)
    assert sim is not None
    assert abs(sim - (-1.0)) < 1e-5

def test_compute_cosine_similarity_orthogonal():
    u = np.zeros(192, dtype=np.float32)
    v = np.zeros(192, dtype=np.float32)
    u[:96] = 1.0
    v[96:] = 1.0
    sim = compute_cosine_similarity(u, v)
    assert sim is not None
    assert abs(sim - 0.0) < 1e-5

def test_compute_cosine_similarity_zero_norm():
    u = np.zeros(192, dtype=np.float32)
    v = np.ones(192, dtype=np.float32)
    sim = compute_cosine_similarity(u, v)
    assert sim is None

def test_compute_cosine_similarity_nan_inf():
    u = np.ones(192, dtype=np.float32)
    u[0] = np.nan
    v = np.ones(192, dtype=np.float32)
    assert compute_cosine_similarity(u, v) is None

    u[0] = np.inf
    assert compute_cosine_similarity(u, v) is None

def test_ecapa_evaluator_synthetic_trials():
    np.random.seed(42)
    genuine_scores = list(np.random.normal(loc=0.75, scale=0.1, size=100))
    impostor_scores = list(np.random.normal(loc=0.10, scale=0.1, size=100))

    evaluator = ECAPAEvaluator(genuine_scores, impostor_scores)
    results = evaluator.evaluate()

    assert results["status"] == "SUCCESS"
    assert results["trial_counts"]["genuine_trials"] == 100
    assert results["trial_counts"]["impostor_trials"] == 100
    assert "eer" in results["eer_analysis"]
    assert "auc" in results["roc_analysis"]
    assert len(results["threshold_sweep"]) == 199

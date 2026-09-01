"""
End-to-End Pipeline Integration Test Suite
Owner: Person 6 (Real-Time Integration & QA Engineer)
"""

import pytest
import numpy as np
from ml.deepfake_detector.pipeline import DeepfakeDetector
from speech.prosody.analyzer import ProsodyAnalyzer
from speech.speaker_verification.verifier import SpeakerVerificationEngine
from security.risk_engine.evaluator import RiskScoringEngine
from integration.streaming.temporal_smoother import TemporalRiskSmoother


def test_deepfake_detector_pipeline():
    detector = DeepfakeDetector()
    # 1 second synthetic sine wave test audio
    t = np.linspace(0, 1.0, 16000, dtype=np.float32)
    dummy_audio = 0.5 * np.sin(2 * np.pi * 440 * t)

    res = detector.predict(dummy_audio)
    assert "synthetic_probability" in res
    assert "bonafide_probability" in res
    assert res["classification"] in ["BONAFIDE", "SPOOF"]
    assert "spectral_metrics" in res
    assert 0.0 <= res["synthetic_probability"] <= 1.0


def test_prosody_analyzer():
    analyzer = ProsodyAnalyzer(sample_rate=16000)
    t = np.linspace(0, 1.0, 16000, dtype=np.float32)
    dummy_audio = 0.5 * np.sin(2 * np.pi * 200 * t)

    res = analyzer.analyze(dummy_audio)
    assert "mean_pitch_f0_hz" in res
    assert "jitter_percent" in res
    assert "prosodic_anomaly_score" in res
    assert 0.0 <= res["prosodic_anomaly_score"] <= 1.0


def test_speaker_verification_flow():
    verifier = SpeakerVerificationEngine()
    t = np.linspace(0, 1.0, 16000, dtype=np.float32)
    audio1 = 0.5 * np.sin(2 * np.pi * 300 * t)

    # Enroll
    enroll_res = verifier.enroll_speaker("TEST_VIP", audio1)
    assert enroll_res["status"] == "success"

    # Verify matching
    verify_res = verifier.verify_speaker("TEST_VIP", audio1)
    assert verify_res["enrolled"] is True
    assert verify_res["verified"] is True
    assert verify_res["similarity_score"] >= 0.90


def test_risk_scoring_engine():
    engine = RiskScoringEngine()
    eval_res = engine.evaluate(
        synthetic_prob=0.85,
        acoustic_anomaly=0.70,
        prosodic_anomaly=0.60,
        speaker_identity_anomaly=0.80,
        context_metadata={"transaction_amount": 600000, "urgency_flag": True}
    )

    assert eval_res["overall_risk_score"] >= 70
    assert eval_res["risk_tier"] in ["HIGH", "CRITICAL"]
    assert eval_res["action_required"] in ["STEP_UP_MFA", "BLOCK_TRANSACTION"]


def test_temporal_smoother():
    smoother = TemporalRiskSmoother(window_size=3, alpha=0.5)
    s1 = smoother.update(80)
    s2 = smoother.update(90)
    assert s2["smoothed_score"] > 80

import os
import wave
import struct
import json
import csv
import numpy as np
import pytest

from evaluation.scripts.evaluate_aasist import (
    evaluate,
    compute_binary_metrics,
    compute_roc_auc,
    compute_brier_score,
    calculate_sha256
)


def create_test_wav_file(filepath: str, sample_rate=16000, duration_sec=1.0, frequency=440.0, silence=False):
    """Creates a valid WAV audio file on disk for evaluator unit tests."""
    num_samples = int(sample_rate * duration_sec)
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with wave.open(filepath, "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        frames = []
        for i in range(num_samples):
            if silence:
                val = 0
            else:
                t = float(i) / sample_rate
                val = int(32767.0 * 0.5 * np.sin(2.0 * np.pi * frequency * t))
            frames.append(struct.pack("<h", val))
        wav_file.writeframes(b"".join(frames))


def test_empty_dataset_handling(tmp_path):
    data_dir = tmp_path / "empty_data"
    output_dir = tmp_path / "results"
    os.makedirs(data_dir / "bonafide", exist_ok=True)
    os.makedirs(data_dir / "spoof", exist_ok=True)

    res = evaluate(data_dir=str(data_dir), output_dir=str(output_dir), checkpoint_path=os.path.join("ml", "deepfake_detector", "checkpoints", "AASIST.pth"))
    assert res["status"] == "DATASET_NOT_AVAILABLE"
    assert res["dataset"]["total_samples"] == 0
    assert os.path.exists(output_dir / "metrics.json")


def test_single_class_bonafide_only(tmp_path):
    data_dir = tmp_path / "bonafide_data"
    output_dir = tmp_path / "results"
    create_test_wav_file(str(data_dir / "bonafide" / "sample1.wav"))

    res = evaluate(data_dir=str(data_dir), output_dir=str(output_dir), checkpoint_path=os.path.join("ml", "deepfake_detector", "checkpoints", "AASIST.pth"))
    assert res["status"] == "SINGLE_CLASS_EVALUATION"
    assert res["dataset"]["total_samples"] == 1
    assert res["dataset"]["bonafide_count"] == 1
    assert res["dataset"]["spoof_count"] == 0
    assert res["overall_metrics"]["eer"] == "SINGLE_CLASS_UNAVAILABLE"


def test_single_class_spoof_only(tmp_path):
    data_dir = tmp_path / "spoof_data"
    output_dir = tmp_path / "results"
    create_test_wav_file(str(data_dir / "spoof" / "sample1.wav"))

    res = evaluate(data_dir=str(data_dir), output_dir=str(output_dir), checkpoint_path=os.path.join("ml", "deepfake_detector", "checkpoints", "AASIST.pth"))
    assert res["status"] == "SINGLE_CLASS_EVALUATION"
    assert res["dataset"]["total_samples"] == 1
    assert res["dataset"]["spoof_count"] == 1


def test_dual_class_dataset_evaluation(tmp_path):
    data_dir = tmp_path / "dual_data"
    output_dir = tmp_path / "results"
    create_test_wav_file(str(data_dir / "bonafide" / "human1.wav"), frequency=440.0)
    create_test_wav_file(str(data_dir / "spoof" / "deepfake1.wav"), frequency=880.0)

    res = evaluate(data_dir=str(data_dir), output_dir=str(output_dir), checkpoint_path=os.path.join("ml", "deepfake_detector", "checkpoints", "AASIST.pth"))
    assert res["status"] == "EVALUATION_COMPLETE"
    assert res["dataset"]["total_samples"] == 2
    assert res["dataset"]["has_both_classes"] is True
    assert 0.0 <= res["overall_metrics"]["roc_auc"] <= 1.0
    assert os.path.exists(output_dir / "metrics.json")
    assert os.path.exists(output_dir / "predictions.csv")
    assert os.path.exists(output_dir / "thresholds.csv")


def test_corrupt_audio_file_handling(tmp_path):
    data_dir = tmp_path / "corrupt_data"
    output_dir = tmp_path / "results"
    bad_file = data_dir / "bonafide" / "corrupt.wav"
    os.makedirs(os.path.dirname(bad_file), exist_ok=True)
    bad_file.write_bytes(b"INVALID_AUDIO_DATA")

    res = evaluate(data_dir=str(data_dir), output_dir=str(output_dir), checkpoint_path=os.path.join("ml", "deepfake_detector", "checkpoints", "AASIST.pth"))
    assert res["dataset"]["total_samples"] == 1
    with open(output_dir / "predictions.csv") as f:
        reader = list(csv.DictReader(f))
        assert "ERROR" in reader[0]["status"]


def test_probability_range_and_threshold_classification():
    y_true = np.array([0, 0, 1, 1])
    y_score = np.array([0.1, 0.4, 0.7, 0.9])

    # Check metrics at threshold 0.5
    m = compute_binary_metrics(y_true, y_score, 0.50)
    assert m["tp"] == 2
    assert m["tn"] == 2
    assert m["fp"] == 0
    assert m["fn"] == 0
    assert m["accuracy"] == 1.0
    assert m["f1_score"] == 1.0

    # Check ROC-AUC calculation
    auc = compute_roc_auc(y_true, y_score)
    assert auc == 1.0


def test_missing_checkpoint_handling(tmp_path):
    data_dir = tmp_path / "data"
    output_dir = tmp_path / "results"
    create_test_wav_file(str(data_dir / "bonafide" / "test.wav"))

    res = evaluate(data_dir=str(data_dir), output_dir=str(output_dir), checkpoint_path="non_existent_path.pth")
    assert res["reproducibility"]["checkpoint_filename"] == "non_existent_path.pth"
    assert res["reproducibility"]["checkpoint_sha256"] == "FILE_NOT_FOUND"


def test_no_audio_bytes_in_output_files(tmp_path):
    data_dir = tmp_path / "data"
    output_dir = tmp_path / "results"
    create_test_wav_file(str(data_dir / "bonafide" / "test.wav"))

    evaluate(data_dir=str(data_dir), output_dir=str(output_dir), checkpoint_path=os.path.join("ml", "deepfake_detector", "checkpoints", "AASIST.pth"))

    with open(output_dir / "predictions.csv", "r", encoding="utf-8") as f:
        csv_text = f.read()
    assert "RIFF" not in csv_text
    assert "WAVE" not in csv_text
    assert len(csv_text) < 10000

    with open(output_dir / "metrics.json", "r", encoding="utf-8") as f:
        json_text = f.read()
    assert "waveform" not in json_text
    assert "raw_pcm" not in json_text

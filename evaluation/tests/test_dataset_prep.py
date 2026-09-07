import os
import json
import pytest
import numpy as np
from evaluation.scripts.prepare_speaker_dataset import (
    discover_speaker_files,
    split_speakers,
    generate_trials,
    prepare_dataset_manifests
)

def test_split_speakers_zero_leakage():
    speaker_ids = [f"SPK_{i:03d}" for i in range(100)]
    dev, heldout = split_speakers(speaker_ids, dev_ratio=0.60, seed=42)

    assert len(dev) == 60
    assert len(heldout) == 40
    # Zero speaker leakage check
    intersection = set(dev).intersection(set(heldout))
    assert len(intersection) == 0

def test_generate_trials_structure():
    speaker_map = {
        "SPK_001": ["audio_A1.wav", "audio_A2.wav", "audio_A3.wav"],
        "SPK_002": ["audio_B1.wav", "audio_B2.wav"],
        "SPK_003": ["audio_C1.wav", "audio_C2.wav"]
    }
    speakers = ["SPK_001", "SPK_002", "SPK_003"]
    trials = generate_trials(speaker_map, speakers, num_genuine_per_speaker=5, num_impostor_per_speaker=5, seed=42)

    assert len(trials) > 0
    genuine_trials = [t for t in trials if t["label"] == "GENUINE"]
    impostor_trials = [t for t in trials if t["label"] == "IMPOSTOR"]

    assert len(genuine_trials) > 0
    assert len(impostor_trials) > 0

    for t in genuine_trials:
        assert t["speaker_id"] == t["claimed_speaker_id"]
        assert t["is_target"] is True

    for t in impostor_trials:
        assert t["speaker_id"] != t["claimed_speaker_id"]
        assert t["is_target"] is False

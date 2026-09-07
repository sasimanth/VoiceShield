import io
import wave
import struct
import os
import torch
import numpy as np
import pytest
from fastapi.testclient import TestClient

from ml_service.main import app
from ml.deepfake_detector.model import AASIST
from ml.deepfake_detector.pipeline import DeepfakeDetector

client = TestClient(app)

def create_wav_bytes(sample_rate=16000, duration_sec=1.0, channels=1, frequency=440.0, silence=False):
    """Generates valid WAV audio bytes in memory for unit testing."""
    num_samples = int(sample_rate * duration_sec)
    buffer = io.BytesIO()

    with wave.open(buffer, "wb") as wav_file:
        wav_file.setnchannels(channels)
        wav_file.setsampwidth(2)  # 16-bit PCM
        wav_file.setframerate(sample_rate)

        frames = []
        for i in range(num_samples):
            if silence:
                value = 0
            else:
                t = float(i) / sample_rate
                value = int(32767.0 * 0.5 * np.sin(2.0 * np.pi * frequency * t))

            for _ in range(channels):
                frames.append(struct.pack("<h", value))

        wav_file.writeframes(b"".join(frames))

    return buffer.getvalue()


def test_aasist_model_import_and_architecture():
    """Verifies that the official AASIST graph attention model instantiates correctly."""
    model = AASIST()
    assert isinstance(model, torch.nn.Module)
    dummy_input = torch.randn(1, 64600)
    with torch.no_grad():
        last_hidden, logits = model(dummy_input)
    assert last_hidden.shape == (1, 160)
    assert logits.shape == (1, 2)


def test_aasist_strict_checkpoint_loading():
    """Verifies strict state_dict checkpoint loading of AASIST.pth."""
    ckpt_path = os.path.join("ml", "deepfake_detector", "checkpoints", "AASIST.pth")
    assert os.path.exists(ckpt_path), f"Checkpoint missing at {ckpt_path}"

    model = AASIST()
    ckpt = torch.load(ckpt_path, map_location="cpu")
    model.load_state_dict(ckpt, strict=True)
    model.eval()

    dummy_input = torch.randn(1, 64600)
    with torch.inference_mode():
        _, logits = model(dummy_input)
        probs = torch.softmax(logits, dim=1)
    
    assert probs.shape == (1, 2)
    assert 0.0 <= probs[0, 0].item() <= 1.0  # SPOOF
    assert 0.0 <= probs[0, 1].item() <= 1.0  # BONAFIDE


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert data["service"] == "voiceshield-ml"
    assert "model_status" in data
    assert "checkpoint_loaded" in data
    assert "ecapa_loaded" in data
    assert "ecapa_status" in data


def test_analyze_valid_mono_wav_with_aasist():
    wav_data = create_wav_bytes(sample_rate=16000, duration_sec=1.5, channels=1)
    response = client.post(
        "/analyze",
        files={"file": ("sample.wav", wav_data, "audio/wav")}
    )
    assert response.status_code == 200
    data = response.json()
    assert "deepfake_probability" in data
    assert "acoustic_anomaly" in data
    assert "status" in data
    assert "model_status" in data
    assert 0.0 <= data["deepfake_probability"] <= 1.0
    assert 0.0 <= data["acoustic_anomaly"] <= 1.0
    assert data["status"] == "SUCCESS"
    assert data["model_status"] == "READY"


def test_analyze_stereo_wav_mono_conversion():
    wav_data = create_wav_bytes(sample_rate=16000, duration_sec=1.0, channels=2)
    response = client.post(
        "/analyze",
        files={"file": ("stereo.wav", wav_data, "audio/wav")}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["acoustic_anomaly"] >= 0.0
    assert data["status"] == "SUCCESS"


def test_analyze_resampling_44100hz():
    wav_data = create_wav_bytes(sample_rate=44100, duration_sec=1.0, channels=1)
    response = client.post(
        "/analyze",
        files={"file": ("resample.wav", wav_data, "audio/wav")}
    )
    assert response.status_code == 200
    assert response.json()["status"] == "SUCCESS"


def test_empty_audio_upload():
    response = client.post(
        "/analyze",
        files={"file": ("empty.wav", b"", "audio/wav")}
    )
    assert response.status_code == 400
    assert "empty" in response.json()["detail"].lower()


def test_corrupt_audio_upload():
    response = client.post(
        "/analyze",
        files={"file": ("corrupt.wav", b"INVALID_RAW_BYTES_NON_AUDIO", "audio/wav")}
    )
    assert response.status_code == 400
    assert "invalid or corrupt" in response.json()["detail"].lower()


def test_silent_audio_upload():
    silent_wav = create_wav_bytes(sample_rate=16000, duration_sec=1.0, channels=1, silence=True)
    response = client.post(
        "/analyze",
        files={"file": ("silent.wav", silent_wav, "audio/wav")}
    )
    assert response.status_code == 400
    assert "silent" in response.json()["detail"].lower()


def test_missing_checkpoint_unconfigured_status():
    detector_missing = DeepfakeDetector(checkpoint_path="non_existent_path.pth")
    assert detector_missing.model_loaded is False
    assert detector_missing.model_status_str == "UNCONFIGURED"

    dummy_audio = np.zeros(16000, dtype=np.float32)
    res = detector_missing.predict(dummy_audio)
    assert res["status"] == "MODEL_UNCONFIGURED"
    assert res["model_status"] == "UNCONFIGURED"
    assert res["deepfake_probability"] == 0.0


def test_corrupt_checkpoint_load_failed_status(tmp_path):
    bad_ckpt = tmp_path / "bad.pth"
    bad_ckpt.write_bytes(b"INVALID_CHECKPOINT_DATA")
    
    detector_bad = DeepfakeDetector(checkpoint_path=str(bad_ckpt))
    assert detector_bad.model_loaded is False
    assert detector_bad.model_status_str == "LOAD_FAILED"

    dummy_audio = np.zeros(16000, dtype=np.float32)
    res = detector_bad.predict(dummy_audio)
    assert res["status"] == "MODEL_LOAD_FAILED"
    assert res["model_status"] == "LOAD_FAILED"
    assert res["deepfake_probability"] == 0.0


def test_speaker_embed_valid_audio():
    wav_data = create_wav_bytes(sample_rate=16000, duration_sec=1.5, channels=1)
    response = client.post(
        "/speaker/embed",
        files={"file": ("speaker.wav", wav_data, "audio/wav")}
    )
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "SUCCESS"
    assert data["embedding_dim"] == 192
    assert isinstance(data["embedding"], list)
    assert len(data["embedding"]) == 192
    assert all(isinstance(v, float) and not np.isnan(v) and not np.isinf(v) for v in data["embedding"])


def test_speaker_embed_short_audio_unprocessable():
    wav_data = create_wav_bytes(sample_rate=16000, duration_sec=0.5, channels=1)
    response = client.post(
        "/speaker/embed",
        files={"file": ("short.wav", wav_data, "audio/wav")}
    )
    assert response.status_code == 422
    assert "below operational minimum" in response.json()["detail"].lower()


def test_speaker_embed_corrupt_audio():
    response = client.post(
        "/speaker/embed",
        files={"file": ("corrupt.wav", b"NOT_A_WAV_FILE", "audio/wav")}
    )
    assert response.status_code == 400
    assert "invalid or corrupt" in response.json()["detail"].lower()

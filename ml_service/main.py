import os
import io
import logging
import numpy as np
import soundfile as sf
import scipy.signal as signal
from typing import Optional, Dict, Any, List

from fastapi import FastAPI, File, UploadFile, HTTPException, status
from pydantic import BaseModel

from ml.deepfake_detector.pipeline import DeepfakeDetector
from ml.deepfake_detector.spectral_features import SpectralFeatureExtractor
from ml.speaker_verifier.ecapa_embedder import ECAPASpeakerEmbedder

logger = logging.getLogger("voiceshield.ml_service")

app = FastAPI(
    title="VoiceShield ML Inference Service",
    version="1.0.0",
    description="AI-Powered Real-Time Voice Integrity & Deepfake Detection Service"
)

# Initialize canonical DeepfakeDetector, SpectralFeatureExtractor, and ECAPASpeakerEmbedder
model_path = os.getenv("DEEPFAKE_MODEL_PATH")
detector = DeepfakeDetector(checkpoint_path=model_path)
spectral_extractor = SpectralFeatureExtractor(sample_rate=16000)

ecapa_model_source = os.getenv("ECAPA_MODEL_SOURCE", "speechbrain/spkrec-ecapa-voxceleb")
ecapa_embedder = ECAPASpeakerEmbedder(model_source=ecapa_model_source)


class FeatureResponse(BaseModel):
    deepfake_probability: float
    acoustic_anomaly: float
    prosodic_anomaly: float = 0.0
    behavioral_anomaly: float = 0.0
    status: str
    model_status: str


class SpeakerEmbeddingResponse(BaseModel):
    embedding: List[float]
    embedding_dim: int = 192
    status: str = "SUCCESS"


def decode_and_preprocess_audio(raw_bytes: bytes, target_sr: int = 16000) -> np.ndarray:
    """Decodes uploaded audio bytes in memory to float32 16kHz mono NumPy array."""
    if not raw_bytes or len(raw_bytes) == 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Empty audio payload provided."
        )

    if len(raw_bytes) > 50 * 1024 * 1024:  # 50 MB limit
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Audio payload exceeds maximum 50 MB limit."
        )

    try:
        data, sr = sf.read(io.BytesIO(raw_bytes), dtype='float32')
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid or corrupt audio file format: {str(e)}"
        )

    if data is None or len(data) == 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Decoded audio payload contains 0 samples."
        )

    # Convert stereo/multi-channel to mono
    if data.ndim > 1:
        data = np.mean(data, axis=1)

    # Check for NaN / Inf
    if np.isnan(data).any() or np.isinf(data).any():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Audio payload contains invalid numerical values (NaN/Inf)."
        )

    # Check for silence (RMS energy check)
    rms = np.sqrt(np.mean(data ** 2))
    if rms < 1e-6:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Audio payload is completely silent."
        )

    # Resample to target sample rate (16,000 Hz) if required
    if sr != target_sr:
        num_samples = int(round(len(data) * target_sr / float(sr)))
        if num_samples > 0:
            data = signal.resample(data, num_samples).astype(np.float32)

    return data


@app.get("/health")
def health_check():
    """Health check endpoint indicating service uptime and neural model readiness."""
    model_ready = detector.model_loaded
    ecapa_ready = ecapa_embedder.model_loaded
    return {
        "status": "UP",
        "service": "voiceshield-ml",
        "model_status": detector.model_status_str,
        "checkpoint_loaded": model_ready,
        "checkpoint_path": detector.checkpoint_path if detector.checkpoint_path else None,
        "ecapa_loaded": ecapa_ready,
        "ecapa_status": ecapa_embedder.model_status_str
    }


@app.post("/analyze", response_model=FeatureResponse)
async def analyze_audio(file: UploadFile = File(...)):
    """Analyzes an uploaded audio file for spectral anomalies and deepfake risk."""
    if not file:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No audio file provided."
        )

    contents = await file.read()
    audio_data = decode_and_preprocess_audio(contents, target_sr=16000)

    # 1. Compute acoustic spectral feature anomalies (Heuristic signal)
    spectral_metrics = spectral_extractor.extract(audio_data)
    acoustic_anomaly = float(spectral_metrics.get("acoustic_anomaly_score", 0.0))

    # 2. Run model inference if a valid trained checkpoint is loaded
    if detector.model_loaded:
        prediction = detector.predict(audio_data)
        deepfake_prob = float(prediction.get("deepfake_probability", 0.0))
        model_status_str = "READY"
        response_status = "SUCCESS"
    else:
        # Untrained / load-failed / unconfigured model state:
        deepfake_prob = 0.0
        model_status_str = detector.model_status_str
        response_status = f"MODEL_{model_status_str}"

    return FeatureResponse(
        deepfake_probability=round(deepfake_prob, 4),
        acoustic_anomaly=round(acoustic_anomaly, 4),
        prosodic_anomaly=0.0,
        behavioral_anomaly=0.0,
        status=response_status,
        model_status=model_status_str
    )


@app.post("/speaker/embed", response_model=SpeakerEmbeddingResponse)
async def extract_speaker_embedding(file: UploadFile = File(...)):
    """Extracts a 192-dimensional ECAPA-TDNN speaker embedding vector from raw WAV audio."""
    if not file:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No audio file provided."
        )

    contents = await file.read()
    audio_data = decode_and_preprocess_audio(contents, target_sr=16000)

    # Operational duration check (1.0 second policy)
    duration_sec = len(audio_data) / 16000.0
    if duration_sec < 1.0:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Audio duration ({duration_sec:.2f}s) is below operational minimum threshold of 1.00s."
        )

    if not ecapa_embedder.model_loaded:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"ECAPA-TDNN model is not loaded (status: {ecapa_embedder.model_status_str})."
        )

    try:
        embedding_np = ecapa_embedder.extract_embedding(audio_data, sample_rate=16000)
        embedding_list = [float(v) for v in embedding_np]
        return SpeakerEmbeddingResponse(
            embedding=embedding_list,
            embedding_dim=len(embedding_list),
            status="SUCCESS"
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"ECAPA-TDNN inference failed: {str(e)}"
        )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)

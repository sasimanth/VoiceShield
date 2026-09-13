
"""
VoiceShield FastAPI ML Service

Responsibilities:
- Receive audio files
- Run deepfake/voice-clone detection
- Generate ECAPA-TDNN speaker embeddings
- Expose ML health/status

Python does NOT:
- Access PostgreSQL
- Store speaker profiles
- Compare reference/probe embeddings
- Calculate speaker cosine similarity
- Make final speaker verification decisions
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

import io
import numpy as np
import soundfile as sf
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.responses import JSONResponse
from scipy.signal import resample_poly

from ml.deepfake_detector.pipeline import DeepfakeDetector
from speech.speaker_verification.ecapa_service import (
    ECAPASpeakerEmbedder,
    EMBEDDING_DIMENSION,
    MODEL_NAME,
)


# ---------------------------------------------------------------------
# FastAPI application
# ---------------------------------------------------------------------

app = FastAPI(
    title="VoiceShield ML Service",
    description="AI voice integrity and speaker embedding service",
    version="1.0.0",
)


# ---------------------------------------------------------------------
# Model paths
# ---------------------------------------------------------------------

PROJECT_ROOT = Path(__file__).resolve().parent.parent

DEEPFAKE_CHECKPOINT = (
    PROJECT_ROOT
    / "checkpoints"
    / "experiment4_best_model.pth"
)


# ---------------------------------------------------------------------
# Deepfake detector
# ---------------------------------------------------------------------

try:
    deepfake_detector = DeepfakeDetector(
        checkpoint_path=str(DEEPFAKE_CHECKPOINT)
    )

    if deepfake_detector.model_loaded:
        deepfake_model_status = "READY"
    else:
        deepfake_model_status = "BASELINE_INITIALIZED"

    deepfake_model_error = None

except Exception as exc:
    deepfake_detector = None
    deepfake_model_status = "UNAVAILABLE"
    deepfake_model_error = str(exc)


# ---------------------------------------------------------------------
# ECAPA speaker embedding model
# ---------------------------------------------------------------------

try:
    embedder = ECAPASpeakerEmbedder()
    speaker_model_status = "READY"
    speaker_model_error = None

except Exception as exc:
    embedder = None
    speaker_model_status = "UNAVAILABLE"
    speaker_model_error = str(exc)


# ---------------------------------------------------------------------
# Audio preprocessing
# ---------------------------------------------------------------------

TARGET_SAMPLE_RATE = 16000


def load_audio_16k_mono(audio_bytes: bytes) -> np.ndarray:
    """
    Decode uploaded audio and convert it to mono 16 kHz float32 audio.
    """

    if not audio_bytes:
        raise ValueError("Uploaded audio file is empty.")

    try:
        audio, sample_rate = sf.read(
            io.BytesIO(audio_bytes),
            dtype="float32",
        )
    except Exception as exc:
        raise ValueError(
            "Unable to decode the uploaded audio file."
        ) from exc

    if audio is None or len(audio) == 0:
        raise ValueError("Uploaded audio contains no samples.")

    # Convert stereo/multi-channel audio to mono.
    if audio.ndim == 2:
        audio = np.mean(audio, axis=1)

    audio = np.asarray(audio, dtype=np.float32)

    # Reject NaN/Infinity.
    if not np.all(np.isfinite(audio)):
        raise ValueError(
            "Audio contains invalid numeric values."
        )

    # Resample to 16 kHz when necessary.
    if sample_rate != TARGET_SAMPLE_RATE:
        audio = resample_poly(
            audio,
            TARGET_SAMPLE_RATE,
            sample_rate,
        ).astype(np.float32)

    # Peak normalization.
    peak = float(np.max(np.abs(audio)))

    if peak > 0.0:
        audio = audio / peak

    return audio.astype(np.float32)


# ---------------------------------------------------------------------
# Health endpoint
# ---------------------------------------------------------------------

@app.get("/health")
def health() -> dict[str, Any]:
    """
    Return the health/status of the ML service and its models.
    """

    service_ready = (
        deepfake_detector is not None
        or embedder is not None
    )

    response: dict[str, Any] = {
        "status": "UP" if service_ready else "DEGRADED",
        "service": "VoiceShield-ML-Service",

        "deepfake_model": {
            "status": deepfake_model_status,
            "checkpoint": (
                str(DEEPFAKE_CHECKPOINT)
                if DEEPFAKE_CHECKPOINT.exists()
                else "not_found"
            ),
            "device": (
                str(deepfake_detector.device)
                if deepfake_detector is not None
                else "unavailable"
            ),
        },

        "speaker_model": {
            "status": speaker_model_status,
            "model": MODEL_NAME,
            "embedding_dimension": EMBEDDING_DIMENSION,
            "device": (
                embedder.device
                if embedder is not None
                else "unavailable"
            ),
        },
    }

    return response


# ---------------------------------------------------------------------
# Deepfake prediction endpoint
# ---------------------------------------------------------------------

@app.post("/ml/predict")
async def ml_predict(
    file: UploadFile = File(...),
) -> dict[str, Any]:
    """
    Run deepfake/voice-clone detection.

    Java receives the AI result and remains responsible for
    business logic, speaker verification, and final risk decisions.
    """

    if deepfake_detector is None:
        raise HTTPException(
            status_code=503,
            detail="Deepfake detection model is unavailable.",
        )

    if not file.filename:
        raise HTTPException(
            status_code=400,
            detail="Audio filename is required.",
        )

    try:
        audio_bytes = await file.read()

        if not audio_bytes:
            raise HTTPException(
                status_code=400,
                detail="Uploaded audio file is empty.",
            )

        audio_16k = load_audio_16k_mono(audio_bytes)

        if len(audio_16k) == 0:
            raise HTTPException(
                status_code=400,
                detail="Uploaded audio contains no usable samples.",
            )

        # Run the actual trained deepfake detector.
        result = deepfake_detector.predict(audio_16k)

        return {
            "success": True,

            # Fields expected by the Java backend.
            "deepfake_probability": result[
                "synthetic_probability"
            ],
            "bonafide_probability": result[
                "bonafide_probability"
            ],
            "classification": result["classification"],

            # Original AI pipeline fields.
            "synthetic_probability": result[
                "synthetic_probability"
            ],
            "spectral_metrics": result[
                "spectral_metrics"
            ],
            "model_status": result["model_status"],

            "source": "Python-DeepfakeDetector",
        }

    except HTTPException:
        raise

    except ValueError as exc:
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        ) from exc

    except Exception as exc:
        print(
            f"[ERROR] Deepfake inference failed: {exc}"
        )

        raise HTTPException(
            status_code=500,
            detail="Deepfake inference failed.",
        ) from exc


# ---------------------------------------------------------------------
# ECAPA speaker embedding endpoint
# ---------------------------------------------------------------------

@app.post("/speaker/embed")
async def speaker_embed(
    file: UploadFile = File(...),
) -> dict[str, Any]:
    """
    Generate a 192-D ECAPA-TDNN speaker embedding.

    Java is responsible for:
    - reference profile lookup
    - cosine similarity
    - threshold application
    - final MATCH/MISMATCH decision
    """

    if embedder is None:
        raise HTTPException(
            status_code=503,
            detail="ECAPA model is unavailable.",
        )

    if not file.filename:
        raise HTTPException(
            status_code=400,
            detail="Audio filename is required.",
        )

    try:
        audio_bytes = await file.read()

        if not audio_bytes:
            raise HTTPException(
                status_code=400,
                detail="Uploaded audio file is empty.",
            )

        embedding = embedder.generate_embedding(audio_bytes)

        # Validate exact ECAPA dimension.
        if embedding.shape[0] != EMBEDDING_DIMENSION:
            raise ValueError(
                "ECAPA embedding has an unexpected dimension."
            )

        # Validate values.
        if not np.all(np.isfinite(embedding)):
            raise ValueError(
                "ECAPA embedding contains invalid values."
            )

        return {
            "success": True,
            "model": MODEL_NAME,
            "embedding_dimension": int(
                embedding.shape[0]
            ),
            "embedding": embedding.tolist(),
        }

    except HTTPException:
        raise

    except ValueError as exc:
        raise HTTPException(
            status_code=400,
            detail=str(exc),
        ) from exc

    except Exception as exc:
        print(
            f"[ERROR] Speaker embedding failed: {exc}"
        )

        raise HTTPException(
            status_code=500,
            detail="Speaker embedding generation failed.",
        ) from exc


# ---------------------------------------------------------------------
# Root endpoint
# ---------------------------------------------------------------------

@app.get("/")
def root() -> dict[str, str]:
    """
    Basic service information.
    """

    return {
        "service": "VoiceShield ML Service",
        "status": "running",
        "docs": "/docs",
    }


# ---------------------------------------------------------------------
# Generic exception handler
# ---------------------------------------------------------------------

@app.exception_handler(Exception)
async def unexpected_exception_handler(
    request,
    exc: Exception,
):
    """
    Prevent internal exception details from being exposed to clients.
    """

    return JSONResponse(
        status_code=500,
        content={
            "success": False,
            "error": "Internal ML service error.",
        },
    )
"""
VoiceShield ECAPA-TDNN Speaker Embedding Service

Architecture:
Audio bytes
    ↓
16 kHz mono preprocessing
    ↓
SpeechBrain ECAPA-TDNN
    ↓
192-D speaker embedding

Python responsibility:
- Audio preprocessing
- ECAPA inference
- Embedding generation

Java responsibility:
- Store reference embedding
- Cosine similarity
- Final verification decision
"""

from __future__ import annotations

import io
import math
from pathlib import Path
from typing import Any

import numpy as np
import soundfile as sf
import torch
from scipy.signal import resample_poly
from speechbrain.inference.speaker import EncoderClassifier
from speechbrain.utils.fetching import LocalStrategy


MODEL_NAME = "speechbrain/spkrec-ecapa-voxceleb"
TARGET_SAMPLE_RATE = 16_000
EMBEDDING_DIMENSION = 192

BASE_DIR = Path(__file__).resolve().parent
MODEL_DIR = BASE_DIR / "weights" / "ecapa_tdnn"


class ECAPASpeakerEmbedder:
    """Generate 192-D speaker embeddings using SpeechBrain ECAPA-TDNN."""

    def __init__(self) -> None:
        self.device = "cuda" if torch.cuda.is_available() else "cpu"

        self.model = EncoderClassifier.from_hparams(
            source=MODEL_NAME,
            savedir=str(MODEL_DIR),
            run_opts={"device": self.device},
            local_strategy=LocalStrategy.COPY,
        )

    def preprocess_audio(self, audio_bytes: bytes) -> torch.Tensor:
        """Decode audio and convert it to 16 kHz mono float32."""

        if not audio_bytes:
            raise ValueError("Audio data is empty.")

        try:
            audio, sample_rate = sf.read(
                io.BytesIO(audio_bytes),
                dtype="float32",
            )
        except Exception as exc:
            raise ValueError("Unable to decode the supplied audio.") from exc

        if audio.size == 0:
            raise ValueError("Decoded audio contains no samples.")

        # Convert stereo/multichannel audio to mono.
        if audio.ndim > 1:
            audio = np.mean(audio, axis=1)

        audio = np.asarray(audio, dtype=np.float32)

        # Replace invalid numerical values before inference.
        if not np.all(np.isfinite(audio)):
            raise ValueError("Audio contains invalid numerical values.")

        # Resample to 16 kHz.
        if sample_rate != TARGET_SAMPLE_RATE:
            gcd = math.gcd(int(sample_rate), TARGET_SAMPLE_RATE)
            up = TARGET_SAMPLE_RATE // gcd
            down = int(sample_rate) // gcd

            audio = resample_poly(
                audio,
                up,
                down,
            ).astype(np.float32)

        # Peak normalization.
        peak = float(np.max(np.abs(audio)))

        if peak > 1e-6:
            audio = audio / peak

        waveform = torch.from_numpy(audio).unsqueeze(0)

        return waveform.to(self.device)

    @torch.inference_mode()
    def generate_embedding(self, audio_bytes: bytes) -> np.ndarray:
        """Generate and validate a normalized 192-D embedding."""

        waveform = self.preprocess_audio(audio_bytes)

        embedding = self.model.encode_batch(waveform)

        # SpeechBrain commonly returns:
        # [batch, 1, embedding_dimension]
        embedding = embedding.squeeze()

        embedding_np = embedding.detach().cpu().numpy().astype(np.float32)

        if embedding_np.ndim != 1:
            embedding_np = embedding_np.reshape(-1)

        if embedding_np.shape[0] != EMBEDDING_DIMENSION:
            raise ValueError(
                f"Unexpected embedding dimension: "
                f"{embedding_np.shape[0]}. "
                f"Expected {EMBEDDING_DIMENSION}."
            )

        if not np.all(np.isfinite(embedding_np)):
            raise ValueError("ECAPA produced non-finite embedding values.")

        norm = float(np.linalg.norm(embedding_np))

        if norm <= 1e-12:
            raise ValueError("ECAPA produced a zero/near-zero embedding.")

        embedding_np = embedding_np / norm

        return embedding_np.astype(np.float32)


if __name__ == "__main__":
    embedder = ECAPASpeakerEmbedder()

    print("Model:", MODEL_NAME)
    print("Device:", embedder.device)
    print("Expected dimension:", EMBEDDING_DIMENSION)
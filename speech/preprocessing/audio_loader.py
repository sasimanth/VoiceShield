"""
Audio Loading, Resampling, Normalization & VAD Preprocessing
Owner: Person 2 (Speech & Speaker Verification Engineer)
"""

import io
import numpy as np
import soundfile as sf
import scipy.signal as signal
from typing import Tuple, Dict, Any


class AudioPreprocessor:
    """Preprocesses raw audio streams or bytes into standardized 16kHz mono float32 waveforms."""

    def __init__(self, target_sr: int = 16000):
        self.target_sr = target_sr

    def load_from_bytes(self, audio_bytes: bytes) -> Tuple[np.ndarray, Dict[str, Any]]:
        """Decodes raw audio bytes (WAV, MP3, FLAC, OGG) to 16kHz mono numpy float32 array."""
        with io.BytesIO(audio_bytes) as bio:
            data, orig_sr = sf.read(bio, dtype="float32")

        # Convert stereo/multichannel to mono
        if data.ndim > 1:
            data = np.mean(data, axis=1)

        # Polyphase Resample to target_sr if necessary
        if orig_sr != self.target_sr:
            gcd = np.gcd(orig_sr, self.target_sr)
            up = self.target_sr // gcd
            down = orig_sr // gcd
            data = signal.resample_poly(data, up, down).astype(np.float32)

        # Peak amplitude normalization
        max_val = np.max(np.abs(data))
        if max_val > 1e-6:
            data = data / max_val

        # Apply simple Energy-based Voice Activity Detection (VAD) to trim leading/trailing silence
        trimmed_data, speech_ratio = self.apply_vad(data)

        metadata = {
            "original_sample_rate": orig_sr,
            "target_sample_rate": self.target_sr,
            "duration_seconds": round(len(trimmed_data) / self.target_sr, 2),
            "speech_activity_ratio": round(speech_ratio, 2)
        }

        return trimmed_data, metadata

    def apply_vad(self, data: np.ndarray, frame_ms: int = 30, energy_thresh: float = 0.02) -> Tuple[np.ndarray, float]:
        """Trims non-speech segments using frame energy thresholding."""
        frame_len = int(self.target_sr * (frame_ms / 1000.0))
        num_frames = len(data) // frame_len
        if num_frames == 0:
            return data, 1.0

        frames = data[:num_frames * frame_len].reshape(num_frames, frame_len)
        energies = np.sqrt(np.mean(frames ** 2, axis=1))
        speech_mask = energies > energy_thresh

        speech_ratio = float(np.mean(speech_mask))
        if np.any(speech_mask):
            first_idx = np.where(speech_mask)[0][0] * frame_len
            last_idx = (np.where(speech_mask)[0][-1] + 1) * frame_len
            trimmed = data[first_idx:last_idx]
            return trimmed, speech_ratio
        return data, speech_ratio

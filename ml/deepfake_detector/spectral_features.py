"""
Spectral Anomaly & Acoustic Biometrics Extractor
Owner: Person 1 (AI/ML Engineer)
"""

import numpy as np
import scipy.signal as signal
from typing import Dict, Any


class SpectralFeatureExtractor:
    """Extracts signal-level acoustic markers indicative of neural TTS synthesis."""

    def __init__(self, sample_rate: int = 16000):
        self.sample_rate = sample_rate

    def extract(self, audio_data: np.ndarray) -> Dict[str, float]:
        """Computes spectral centroid, flatness, rolloff, and high-frequency energy ratio."""
        if len(audio_data) == 0:
            return {
                "spectral_centroid_hz": 0.0,
                "spectral_flatness": 0.0,
                "spectral_rolloff_hz": 0.0,
                "zero_crossing_rate": 0.0,
                "high_freq_energy_ratio": 0.0,
                "acoustic_anomaly_score": 0.0
            }

        # Compute Magnitude Spectrum using STFT
        frequencies, times, stft_matrix = signal.stft(
            audio_data, fs=self.sample_rate, nperseg=512, noverlap=256
        )
        magnitude = np.abs(stft_matrix)
        avg_spectrum = np.mean(magnitude, axis=1) + 1e-10

        # 1. Spectral Centroid
        centroid = float(np.sum(frequencies * avg_spectrum) / np.sum(avg_spectrum))

        # 2. Spectral Flatness (Geometric Mean / Arithmetic Mean)
        geometric_mean = np.exp(np.mean(np.log(avg_spectrum)))
        arithmetic_mean = np.mean(avg_spectrum)
        flatness = float(geometric_mean / (arithmetic_mean + 1e-10))

        # 3. Spectral Rolloff (85% energy threshold)
        cumulative_energy = np.cumsum(avg_spectrum)
        total_energy = cumulative_energy[-1]
        rolloff_idx = np.searchsorted(cumulative_energy, 0.85 * total_energy)
        rolloff = float(frequencies[min(rolloff_idx, len(frequencies) - 1)])

        # 4. Zero Crossing Rate
        zcr = float(np.mean(np.abs(np.diff(np.sign(audio_data)))) / 2.0)

        # 5. High-Frequency Energy Ratio (> 4000 Hz)
        high_freq_mask = frequencies >= 4000
        high_freq_energy = float(np.sum(avg_spectrum[high_freq_mask]) / (total_energy + 1e-10))

        # Heuristic anomaly indicator (synthetic neural vocoders often have unnaturally flat high bands)
        anomaly_score = float(np.clip((flatness * 2.5) + (high_freq_energy * 1.5), 0.0, 1.0))

        return {
            "spectral_centroid_hz": round(centroid, 2),
            "spectral_flatness": round(flatness, 4),
            "spectral_rolloff_hz": round(rolloff, 2),
            "zero_crossing_rate": round(zcr, 4),
            "high_freq_energy_ratio": round(high_freq_energy, 4),
            "acoustic_anomaly_score": round(anomaly_score, 4)
        }

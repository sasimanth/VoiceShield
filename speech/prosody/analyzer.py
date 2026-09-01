"""
Prosody & Behavioral Speech Analysis Engine
Owner: Person 2 (Speech & Speaker Verification Engineer)
"""

import numpy as np
import scipy.signal as signal
from typing import Dict, Any, List


class ProsodyAnalyzer:
    """
    Extracts prosodic dynamics: Pitch (F0) contours, Jitter (pitch perturbation),
    Shimmer (amplitude perturbation), and Pause-to-Speech rhythm patterns.
    """

    def __init__(self, sample_rate: int = 16000):
        self.sample_rate = sample_rate

    def analyze(self, audio_data: np.ndarray) -> Dict[str, Any]:
        """Performs full prosodic and behavioral rhythm analysis."""
        if len(audio_data) < self.sample_rate * 0.2:
            return {
                "mean_pitch_f0_hz": 0.0,
                "pitch_std_dev": 0.0,
                "jitter_percent": 0.0,
                "shimmer_percent": 0.0,
                "pause_count": 0,
                "prosodic_anomaly_score": 0.0
            }

        # Frame parameters (30ms frame, 15ms hop)
        frame_len = int(self.sample_rate * 0.03)
        hop_len = int(self.sample_rate * 0.015)
        num_frames = max(1, (len(audio_data) - frame_len) // hop_len)

        f0_list: List[float] = []
        amplitudes: List[float] = []

        # Autocorrelation based F0 Pitch extraction (Human voice range 70Hz - 450Hz)
        min_lag = int(self.sample_rate / 450.0)
        max_lag = int(self.sample_rate / 70.0)

        for i in range(num_frames):
            frame = audio_data[i * hop_len : i * hop_len + frame_len]
            frame_amp = np.max(np.abs(frame))
            amplitudes.append(frame_amp)

            if frame_amp < 0.03:
                continue

            # Autocorrelation
            corr = np.correlate(frame, frame, mode="full")
            corr = corr[len(corr) // 2 :]
            if len(corr) > max_lag:
                peak_lag = min_lag + np.argmax(corr[min_lag:max_lag])
                if corr[peak_lag] > 0.3 * corr[0]:
                    f0 = self.sample_rate / peak_lag
                    f0_list.append(f0)

        mean_f0 = float(np.mean(f0_list)) if f0_list else 120.0
        std_f0 = float(np.std(f0_list)) if f0_list else 10.0

        # Jitter: Relative pitch period deviation
        if len(f0_list) > 2:
            periods = 1.0 / np.array(f0_list)
            jitter = float(np.mean(np.abs(np.diff(periods))) / (np.mean(periods) + 1e-6) * 100.0)
        else:
            jitter = 0.5

        # Shimmer: Relative peak amplitude deviation
        if len(amplitudes) > 2:
            shimmer = float(np.mean(np.abs(np.diff(amplitudes))) / (np.mean(amplitudes) + 1e-6) * 100.0)
        else:
            shimmer = 2.0

        # Pause detection (frames with amplitude < 0.02)
        pauses = int(np.sum(np.array(amplitudes) < 0.02))

        # Synthetic speech often exhibits unnaturally robotic flat pitch (low std_f0) or abnormal jitter
        prosodic_anomaly = 0.0
        if std_f0 < 8.0:
            prosodic_anomaly += 0.35
        if jitter < 0.20 or jitter > 4.5:
            prosodic_anomaly += 0.35
        if shimmer < 1.0 or shimmer > 15.0:
            prosodic_anomaly += 0.30
        prosodic_anomaly = float(np.clip(prosodic_anomaly, 0.0, 1.0))

        return {
            "mean_pitch_f0_hz": round(mean_f0, 2),
            "pitch_std_dev": round(std_f0, 2),
            "jitter_percent": round(jitter, 3),
            "shimmer_percent": round(shimmer, 3),
            "pause_count": pauses,
            "prosodic_anomaly_score": round(prosodic_anomaly, 4)
        }

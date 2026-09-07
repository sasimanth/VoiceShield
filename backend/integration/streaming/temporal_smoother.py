"""
Real-Time Temporal Smoothing & Sliding Window Buffer
Owner: Person 6 (Real-Time Integration & QA Engineer)
"""

from collections import deque
import numpy as np
from typing import List, Dict, Any


class TemporalRiskSmoother:
    """
    Applies Exponential Moving Average (EMA) and sliding window smoothing across
    continuous audio chunks to prevent false positive spikes during live calls.
    """

    def __init__(self, window_size: int = 5, alpha: float = 0.4):
        self.window_size = window_size
        self.alpha = alpha  # EMA smoothing factor
        self.history: deque = deque(maxlen=window_size)
        self.current_smoothed_score: float = 0.0

    def update(self, raw_chunk_risk_score: float) -> Dict[str, Any]:
        """Updates sliding window with latest chunk score and returns smoothed trajectory."""
        self.history.append(raw_chunk_risk_score)

        if len(self.history) == 1:
            self.current_smoothed_score = float(raw_chunk_risk_score)
        else:
            # EMA: S_t = alpha * Y_t + (1 - alpha) * S_{t-1}
            self.current_smoothed_score = (self.alpha * raw_chunk_risk_score) + ((1.0 - self.alpha) * self.current_smoothed_score)

        smoothed_int = int(round(self.current_smoothed_score))

        return {
            "raw_chunk_score": int(round(raw_chunk_risk_score)),
            "smoothed_score": smoothed_int,
            "trend": "ESCALATING" if len(self.history) > 1 and self.history[-1] > self.history[0] else "STABLE",
            "window_history": list(self.history)
        }

    def reset(self) -> None:
        """Resets the smoother for a new call session."""
        self.history.clear()
        self.current_smoothed_score = 0.0

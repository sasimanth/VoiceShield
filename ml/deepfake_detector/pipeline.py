"""
Deepfake Detection Pipeline (Inference & Scoring) using Official AASIST.

Reference Paper:
Jung et al., "AASIST: Audio Anti-Spoofing using Integrated Spectro-Temporal Graph Attention Networks", ICASSP 2022.
"""

import os
import numpy as np
import torch
import torch.nn.functional as F
from typing import Dict, Any, Optional

from ml.deepfake_detector.model import AASIST
from ml.deepfake_detector.spectral_features import SpectralFeatureExtractor


class DeepfakeDetector:
    """Manages official AASIST model inference and spectral feature extraction."""

    def __init__(self, checkpoint_path: Optional[str] = None):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = AASIST().to(self.device)
        self.spectral_extractor = SpectralFeatureExtractor()
        self.model_loaded = False
        self.model_status_str = "UNCONFIGURED"

        # Determine checkpoint path (parameter or environment variable or default)
        if checkpoint_path is None:
            checkpoint_path = os.getenv(
                "DEEPFAKE_MODEL_PATH",
                os.path.join("ml", "deepfake_detector", "checkpoints", "AASIST.pth")
            )
        self.checkpoint_path = checkpoint_path

        if checkpoint_path and os.path.exists(checkpoint_path):
            try:
                state_dict = torch.load(checkpoint_path, map_location=self.device)
                # Strict state_dict verification against official AASIST architecture
                self.model.load_state_dict(state_dict, strict=True)
                self.model.eval()
                self.model_loaded = True
                self.model_status_str = "READY"
                print(f"[INFO] Successfully loaded official AASIST checkpoint from: {checkpoint_path}")
            except Exception as e:
                self.model_loaded = False
                self.model_status_str = "LOAD_FAILED"
                print(f"[ERROR] Failed strict state_dict loading of AASIST checkpoint at '{checkpoint_path}': {e}")
        else:
            self.model.eval()
            self.model_loaded = False
            self.model_status_str = "UNCONFIGURED"
            print(f"[WARN] No model checkpoint found at '{checkpoint_path}'. Model status will be UNCONFIGURED.")

    def predict(self, audio_data: np.ndarray) -> Dict[str, Any]:
        """
        Runs deepfake inference on preprocessed 16kHz audio using official AASIST.

        ASVspoof / AASIST Class Mapping:
            - Class Index 0: SPOOF (AI Synthetic / Voice Clone)
            - Class Index 1: BONAFIDE (Genuine Human Speech)

        Returns dict containing:
            - deepfake_probability: float [0.0, 1.0] (Class Index 0)
            - bonafide_probability: float [0.0, 1.0] (Class Index 1)
            - status: "SUCCESS", "MODEL_UNCONFIGURED", or "MODEL_LOAD_FAILED"
            - model_status: "READY", "UNCONFIGURED", or "LOAD_FAILED"
        """
        spectral_metrics = self.spectral_extractor.extract(audio_data)

        if not self.model_loaded:
            return {
                "deepfake_probability": 0.0,
                "bonafide_probability": 0.0,
                "status": f"MODEL_{self.model_status_str}",
                "model_status": self.model_status_str,
                "spectral_metrics": spectral_metrics
            }

        # Target length for AASIST is 64,600 samples (~4.0375 seconds at 16,000 Hz)
        target_len = 64600
        if len(audio_data) < target_len:
            repeats = int(np.ceil(target_len / len(audio_data)))
            padded = np.tile(audio_data, repeats)[:target_len]
        else:
            padded = audio_data[:target_len]

        tensor_x = torch.tensor(padded, dtype=torch.float32).unsqueeze(0).to(self.device)

        with torch.inference_mode():
            last_hidden, logits = self.model(tensor_x)
            probabilities = F.softmax(logits, dim=1).squeeze(0)

            # Class Index 0 = SPOOF (Deepfake), Class Index 1 = BONAFIDE (Human)
            deepfake_prob = float(probabilities[0].item())
            bonafide_prob = float(probabilities[1].item())

        return {
            "deepfake_probability": round(deepfake_prob, 4),
            "bonafide_probability": round(bonafide_prob, 4),
            "status": "SUCCESS",
            "model_status": "READY",
            "spectral_metrics": spectral_metrics
        }

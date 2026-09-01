"""
Deepfake Detection Pipeline (Inference & Scoring)
Owner: Person 1 (AI/ML Engineer)
"""

import os
import torch
import torch.nn.functional as F
import numpy as np
from typing import Dict, Any, Optional
from ml.deepfake_detector.model import DeepfakeAASISTModel
from ml.deepfake_detector.spectral_features import SpectralFeatureExtractor


class DeepfakeDetector:
    """Manages deepfake model inference and feature extraction."""

    def __init__(self, checkpoint_path: Optional[str] = None):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model = DeepfakeAASISTModel().to(self.device)
        self.spectral_extractor = SpectralFeatureExtractor()
        self.model_loaded = False
        self.checkpoint_path = checkpoint_path

        if checkpoint_path and os.path.exists(checkpoint_path):
            try:
                state_dict = torch.load(checkpoint_path, map_location=self.device)
                self.model.load_state_dict(state_dict)
                self.model.eval()
                self.model_loaded = True
            except Exception as e:
                print(f"[Warning] Failed to load checkpoint: {e}")
        else:
            self.model.eval()

    def predict(self, audio_data: np.ndarray) -> Dict[str, Any]:
        """
        Runs deepfake inference on processed 16kHz audio.
        Returns:
            - synthetic_probability (0.0 to 1.0)
            - bonafide_probability (0.0 to 1.0)
            - classification ("BONAFIDE" or "SPOOF")
            - spectral_metrics
        """
        spectral_metrics = self.spectral_extractor.extract(audio_data)

        # Prepare audio tensor (target 64600 samples ~ 4 seconds)
        target_len = 64600
        if len(audio_data) < target_len:
            # Repeat to pad
            repeats = int(np.ceil(target_len / len(audio_data)))
            padded = np.tile(audio_data, repeats)[:target_len]
        else:
            padded = audio_data[:target_len]

        tensor_x = torch.tensor(padded, dtype=torch.float32).unsqueeze(0).to(self.device)

        with torch.no_grad():
            logits, embedding = self.model(tensor_x)
            probabilities = F.softmax(logits, dim=1).cpu().numpy()[0]
            # Class 0: Bonafide (Human), Class 1: Spoof (AI Clone)
            bonafide_prob = float(probabilities[0])
            synthetic_prob = float(probabilities[1])

        classification = "SPOOF" if synthetic_prob >= 0.50 else "BONAFIDE"

        return {
            "synthetic_probability": round(synthetic_prob, 4),
            "bonafide_probability": round(bonafide_prob, 4),
            "classification": classification,
            "spectral_metrics": spectral_metrics,
            "model_status": "pretrained_loaded" if self.model_loaded else "baseline_initialized"
        }

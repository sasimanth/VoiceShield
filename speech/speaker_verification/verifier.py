"""
Speaker Enrollment & Cross-Session Verification Engine
Owner: Person 2 (Speech & Speaker Verification Engineer)
"""

import numpy as np
import scipy.signal as signal
from typing import Dict, Any, Optional, List


class SpeakerVerificationEngine:
    """
    Computes voice acoustic identity embeddings and compares incoming call audio
    against enrolled genuine speaker voice profiles using Cosine Similarity.
    """

    def __init__(self):
        # In-memory speaker profile store: speaker_id -> reference_embedding (128-dim vector)
        self.enrolled_profiles: Dict[str, np.ndarray] = {}

    def extract_embedding(self, audio_data: np.ndarray, sample_rate: int = 16000) -> np.ndarray:
        """
        Extracts a normalized 128-dimensional acoustic identity embedding.
        (Can be linked to pretrained ECAPA-TDNN / x-vectors).
        """
        if len(audio_data) < sample_rate * 0.5:
            # Return zero vector if audio is too short
            return np.zeros(128, dtype=np.float32)

        # Compute Mel filterbank energy statistics across 128 frequency bands
        frequencies, times, stft_matrix = signal.stft(
            audio_data, fs=sample_rate, nperseg=512, noverlap=256
        )
        magnitude = np.abs(stft_matrix)  # (257, time)

        # Bin down to 128 dimensions and compute mean & variance
        binned = np.mean(magnitude[:128, :], axis=1)
        norm = np.linalg.norm(binned)
        if norm > 1e-6:
            binned = binned / norm

        return binned.astype(np.float32)

    def enroll_speaker(self, speaker_id: str, audio_data: np.ndarray) -> Dict[str, Any]:
        """Registers a genuine reference voice embedding for a user/executive."""
        embedding = self.extract_embedding(audio_data)
        self.enrolled_profiles[speaker_id] = embedding
        return {
            "status": "success",
            "speaker_id": speaker_id,
            "embedding_dimension": len(embedding),
            "message": f"Speaker profile for '{speaker_id}' enrolled successfully."
        }

    def verify_speaker(self, speaker_id: str, audio_data: np.ndarray, threshold: float = 0.75) -> Dict[str, Any]:
        """
        Compares incoming audio embedding with enrolled voice profile.
        Returns similarity score (0.0 to 1.0) and match decision.
        """
        if speaker_id not in self.enrolled_profiles:
            return {
                "speaker_id": speaker_id,
                "enrolled": False,
                "similarity_score": None,
                "verified": False,
                "message": f"No enrolled voice profile found for speaker '{speaker_id}'."
            }

        ref_emb = self.enrolled_profiles[speaker_id]
        curr_emb = self.extract_embedding(audio_data)

        # Cosine similarity: (A · B) / (||A|| * ||B||)
        dot_product = np.dot(ref_emb, curr_emb)
        norm_product = (np.linalg.norm(ref_emb) * np.linalg.norm(curr_emb)) + 1e-8
        similarity = float(np.clip(dot_product / norm_product, 0.0, 1.0))

        is_match = similarity >= threshold

        return {
            "speaker_id": speaker_id,
            "enrolled": True,
            "similarity_score": round(similarity, 4),
            "similarity_threshold": threshold,
            "verified": is_match,
            "identity_anomaly_score": round(1.0 - similarity, 4),
            "status": "MATCH" if is_match else "IMPERSONATION_MISMATCH"
        }

"""
Speaker Enrollment & Cross-Session Verification Engine
Owner: Person 2 (Speech & Speaker Verification Engineer)
"""

from typing import Dict, Any, List

import torch
import torch.nn.functional as F
from speechbrain.inference.speaker import SpeakerRecognition

from preprocessing.audio_loader import AudioPreprocessor


class SpeakerVerificationEngine:

    def __init__(self):
        print("Loading pretrained speaker verification model...")

        self.verifier = SpeakerRecognition.from_hparams(
            source="speechbrain/spkrec-ecapa-voxceleb",
            savedir="pretrained_models/spkrec-ecapa-voxceleb",
        )

        print("Speaker verification model loaded!")

        # Stores one averaged ECAPA embedding for each enrolled speaker
        self.enrolled_profiles: Dict[str, torch.Tensor] = {}

        self.preprocessor = AudioPreprocessor()

    def extract_embedding(self, audio_path: str) -> torch.Tensor:
        """Extract an ECAPA speaker embedding from an audio file."""

        with open(audio_path, "rb") as audio_file:
            audio_bytes = audio_file.read()

        audio_data, _ = self.preprocessor.load_from_bytes(audio_bytes)

        audio_tensor = (
            torch.from_numpy(audio_data)
            .float()
            .unsqueeze(0)
        )

        embedding = self.verifier.encode_batch(audio_tensor)

        return embedding.squeeze().detach().cpu()

    def enroll_speaker(
        self,
        speaker_id: str,
        audio_paths: List[str]
    ) -> Dict[str, Any]:
        """
        Enroll a speaker using multiple voice recordings.

        Each recording produces an ECAPA embedding.
        The embeddings are averaged to create one speaker profile.
        """

        if not audio_paths:
            return {
                "status": "error",
                "speaker_id": speaker_id,
                "message": "No audio recordings provided."
            }

        embeddings = []

        for audio_path in audio_paths:

            embedding = self.extract_embedding(audio_path)

            # Normalize each embedding before averaging
            embedding = F.normalize(
                embedding,
                p=2,
                dim=0
            )

            embeddings.append(embedding)

        # Stack all embeddings
        embedding_matrix = torch.stack(embeddings)

        # Create average speaker profile
        speaker_profile = embedding_matrix.mean(dim=0)

        # Normalize final profile
        speaker_profile = F.normalize(
            speaker_profile,
            p=2,
            dim=0
        )

        self.enrolled_profiles[speaker_id] = speaker_profile

        return {
            "status": "success",
            "speaker_id": speaker_id,
            "recordings_used": len(audio_paths),
            "embedding_dimension": speaker_profile.shape[0],
            "message": (
                f"Speaker profile for '{speaker_id}' "
                f"created using {len(audio_paths)} recordings."
            )
        }

    def verify_speaker(
        self,
        speaker_id: str,
        audio_path: str,
        threshold: float = 0.30
    ) -> Dict[str, Any]:
        """Compare a new recording against the enrolled speaker profile."""

        # Check whether speaker is enrolled
        if speaker_id not in self.enrolled_profiles:
            return {
                "speaker_id": speaker_id,
                "enrolled": False,
                "similarity_score": None,
                "verified": False,
                "message": (
                    f"No enrolled voice profile found "
                    f"for speaker '{speaker_id}'."
                )
            }

        speaker_profile = self.enrolled_profiles[speaker_id]

        # --------------------------------------------------
        # Check whether the recording contains enough speech
        # --------------------------------------------------

        with open(audio_path, "rb") as audio_file:
            audio_bytes = audio_file.read()

        audio_data, audio_metadata = self.preprocessor.load_from_bytes(
            audio_bytes
        )

        duration = audio_metadata["duration_seconds"]
        speech_ratio = audio_metadata["speech_activity_ratio"]

        # Very short or mostly silent audio should not be
        # treated as an impersonation attempt.
        if duration < 1.0 or speech_ratio < 0.30:
            return {
                "speaker_id": speaker_id,
                "enrolled": True,
                "similarity_score": None,
                "similarity_threshold": threshold,
                "verified": False,
                "identity_anomaly_score": None,
                "status": "INSUFFICIENT_AUDIO",
                "message": (
                    "Not enough usable speech for reliable "
                    "speaker verification."
                )
            }

        # --------------------------------------------------
        # Extract current speaker embedding
        # --------------------------------------------------

        current_embedding = self.extract_embedding(audio_path)

        current_embedding = F.normalize(
            current_embedding,
            p=2,
            dim=0
        )

        # --------------------------------------------------
        # Calculate similarity
        # --------------------------------------------------

        similarity = F.cosine_similarity(
            speaker_profile.unsqueeze(0),
            current_embedding.unsqueeze(0)
        ).item()

        is_match = similarity >= threshold

        identity_anomaly_score = max(
            0.0,
            1.0 - similarity
        )

        return {
            "speaker_id": speaker_id,
            "enrolled": True,
            "similarity_score": round(similarity, 4),
            "similarity_threshold": threshold,
            "verified": is_match,
            "identity_anomaly_score": round(
                identity_anomaly_score,
                4
            ),
            "status": (
                "MATCH"
                if is_match
                else "IMPERSONATION_MISMATCH"
            )
        }
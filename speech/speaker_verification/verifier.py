"""
Speaker Enrollment & Cross-Session Verification Engine
Owner: Person 2 (Speech & Speaker Verification Engineer)
"""

from typing import Dict, Any
import torch
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

        self.enrolled_profiles: Dict[str, str] = {}
        self.preprocessor = AudioPreprocessor()
        
    def extract_embedding(self, audio_path: str):
        """Extract an ECAPA speaker embedding from an audio file."""

        audio_data, _ = self.preprocessor.load_from_bytes(
            open(audio_path, "rb").read()
        )

        audio_tensor = torch.from_numpy(audio_data).float().unsqueeze(0)

        embedding = self.verifier.encode_batch(audio_tensor)

        return embedding.squeeze().detach().cpu()

    def enroll_speaker(self, speaker_id: str, audio_path: str) -> Dict[str, Any]:
        """Store the reference audio path for a speaker."""

        self.enrolled_profiles[speaker_id] = audio_path

        return {
            "status": "success",
            "speaker_id": speaker_id,
            "message": f"Speaker profile for '{speaker_id}' enrolled successfully."
        }

    def verify_speaker(
        self,
        speaker_id: str,
        audio_path: str,
        threshold: float = 0.5
    ) -> Dict[str, Any]:

        if speaker_id not in self.enrolled_profiles:
            return {
                "speaker_id": speaker_id,
                "enrolled": False,
                "similarity_score": None,
                "verified": False,
                "message": f"No enrolled voice profile found for speaker '{speaker_id}'."
            }

        reference_audio = self.enrolled_profiles[speaker_id]

        reference_data, _ = self.preprocessor.load_from_bytes(
            open(reference_audio, "rb").read()
        )

        current_data, _ = self.preprocessor.load_from_bytes(
            open(audio_path, "rb").read()
        )

        reference_tensor = torch.from_numpy(reference_data).float().unsqueeze(0)
        current_tensor = torch.from_numpy(current_data).float().unsqueeze(0)

        score, prediction = self.verifier.verify_batch(
            reference_tensor,
            current_tensor
        )

        similarity = float(score)

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
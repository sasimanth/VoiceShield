"""
Speaker Enrollment & Cross-Session Verification Engine
Owner: Person 2 (Speech & Speaker Verification Engineer)
"""

from typing import Dict, Any
from speechbrain.inference.speaker import SpeakerRecognition


class SpeakerVerificationEngine:

    def __init__(self):
        print("Loading pretrained speaker verification model...")

        self.verifier = SpeakerRecognition.from_hparams(
    source="speechbrain/spkrec-ecapa-voxceleb",
    savedir="pretrained_models/spkrec-ecapa-voxceleb",
        )
        print("Speaker verification model loaded!")

        self.enrolled_profiles: Dict[str, str] = {}

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

        score, prediction = self.verifier.verify_files(
            reference_audio,
            audio_path
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
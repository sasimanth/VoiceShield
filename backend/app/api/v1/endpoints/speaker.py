"""
Speaker Enrollment & Verification REST Endpoints
Owner: Person 3 (Backend Engineer)
"""

from fastapi import APIRouter, UploadFile, File, Form, HTTPException
from speech.preprocessing.audio_loader import AudioPreprocessor
from speech.speaker_verification.verifier import SpeakerVerificationEngine
from backend.app.schemas.audio import SpeakerVerificationResponse

router = APIRouter()

preprocessor = AudioPreprocessor(target_sr=16000)
speaker_engine = SpeakerVerificationEngine()


@router.post("/enroll", summary="Enroll Speaker Voice Profile")
async def enroll_speaker(
    speaker_id: str = Form(...),
    file: UploadFile = File(...)
):
    """Enrolls genuine baseline voice embedding for a designated VIP, executive, or banking customer."""
    audio_bytes = await file.read()
    if len(audio_bytes) == 0:
        raise HTTPException(status_code=400, detail="Audio file cannot be empty.")

    audio_array, _ = preprocessor.load_from_bytes(audio_bytes)
    result = speaker_engine.enroll_speaker(speaker_id, audio_array)
    return result


@router.post("/verify", response_model=SpeakerVerificationResponse, summary="Verify Speaker Identity")
async def verify_speaker(
    speaker_id: str = Form(...),
    file: UploadFile = File(...)
):
    """Compares sample audio against enrolled reference embedding."""
    audio_bytes = await file.read()
    if len(audio_bytes) == 0:
        raise HTTPException(status_code=400, detail="Audio file cannot be empty.")

    audio_array, _ = preprocessor.load_from_bytes(audio_bytes)
    result = speaker_engine.verify_speaker(speaker_id, audio_array)
    return result

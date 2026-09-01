"""
API v1 Router Aggregator
Owner: Person 3 (Backend Engineer)
"""

from fastapi import APIRouter
from backend.app.api.v1.endpoints import audio, speaker, stream

api_router = APIRouter()
api_router.include_router(audio.router, prefix="/audio", tags=["Audio Authenticity & Deepfake Detection"])
api_router.include_router(speaker.router, prefix="/speaker", tags=["Speaker Verification & Enrollment"])
api_router.include_router(stream.router, prefix="/ws", tags=["Real-Time Live Audio Streaming"])

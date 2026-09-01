"""
Application Configuration & Environment Settings
Owner: Person 3 (Backend Engineer)
"""

from pydantic_settings import BaseSettings
from typing import List, Union
from pydantic import field_validator
import json


class Settings(BaseSettings):
    PROJECT_NAME: str = "VoiceShield"
    VERSION: str = "1.0.0"
    ENVIRONMENT: str = "development"
    DEBUG: bool = True

    API_V1_STR: str = "/api/v1"
    CORS_ORIGINS: Union[List[str], str] = ["http://localhost:5173", "http://localhost:3000"]

    # Security
    JWT_SECRET: str = "super-secret-jwt-key-for-voiceshield-sih-2026-change-in-prod"
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60

    # Model Checkpoints
    DEEPFAKE_MODEL_PATH: str = "ml/checkpoints/deepfake_detector_aasist.pt"
    SPEAKER_MODEL_PATH: str = "speech/speaker_verification/weights/ecapa_tdnn.pt"

    @field_validator("CORS_ORIGINS", mode="before")
    def assemble_cors_origins(cls, v: Union[str, List[str]]) -> List[str]:
        if isinstance(v, str) and not v.startswith("["):
            return [i.strip() for i in v.split(",")]
        elif isinstance(v, str):
            return json.loads(v)
        return v

    class Config:
        env_file = ".env"
        extra = "ignore"


settings = Settings()

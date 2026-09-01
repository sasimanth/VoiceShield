"""
Data Transfer Object (DTO) Schemas
Owner: Person 3 (Backend Engineer)
"""

from pydantic import BaseModel, Field
from typing import Dict, Any, List, Optional


class ContextMetadataSchema(BaseModel):
    caller_id: Optional[str] = "UNKNOWN"
    claimed_speaker_id: Optional[str] = None
    transaction_amount: Optional[float] = None
    is_international: bool = False
    caller_trust_score: float = Field(default=1.0, ge=0.0, le=1.0)
    urgency_flag: bool = False
    is_new_beneficiary: bool = False


class AudioAnalysisResponse(BaseModel):
    session_id: str
    overall_risk_score: int
    risk_tier: str
    action_required: str
    recommendations: List[str]
    classification: str
    synthetic_probability: float
    bonafide_probability: float
    spectral_biometrics: Dict[str, Any]
    prosodic_dynamics: Dict[str, Any]
    speaker_verification: Optional[Dict[str, Any]] = None
    context_threats: List[str]
    compliance: Dict[str, Any]


class SpeakerEnrollmentRequest(BaseModel):
    speaker_id: str
    full_name: str
    department_or_role: Optional[str] = None


class SpeakerVerificationResponse(BaseModel):
    speaker_id: str
    enrolled: bool
    similarity_score: Optional[float] = None
    verified: bool
    status: str
    message: str

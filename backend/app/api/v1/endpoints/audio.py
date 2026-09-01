"""
Audio Analysis REST Endpoints
Owner: Person 3 (Backend Engineer)
"""

from fastapi import APIRouter, UploadFile, File, Form, HTTPException, status
import json
from typing import Optional
from datetime import datetime

from speech.preprocessing.audio_loader import AudioPreprocessor
from ml.deepfake_detector.pipeline import DeepfakeDetector
from speech.prosody.analyzer import ProsodyAnalyzer
from speech.speaker_verification.verifier import SpeakerVerificationEngine
from security.risk_engine.evaluator import RiskScoringEngine
from security.compliance.privacy import PrivacyGuard
from backend.app.schemas.audio import AudioAnalysisResponse

router = APIRouter()

# Instantiate core engines
preprocessor = AudioPreprocessor(target_sr=16000)
deepfake_detector = DeepfakeDetector()
prosody_analyzer = ProsodyAnalyzer(sample_rate=16000)
speaker_engine = SpeakerVerificationEngine()
risk_engine = RiskScoringEngine()


@router.post("/analyze", response_model=AudioAnalysisResponse, summary="Analyze Voice Recording")
async def analyze_voice(
    file: UploadFile = File(...),
    claimed_speaker_id: Optional[str] = Form(None),
    caller_id: Optional[str] = Form("CALLER-ANON"),
    transaction_amount: Optional[float] = Form(None),
    urgency_flag: Optional[bool] = Form(False),
    is_new_beneficiary: Optional[bool] = Form(False),
    is_international: Optional[bool] = Form(False)
):
    """
    Ingests raw audio file, runs multi-layer authenticity analysis (Acoustic + Deepfake + Prosody + Speaker Verification),
    evaluates contextual transaction risk, and returns explainable risk score and alerts.
    """
    if not file.filename:
        raise HTTPException(status_code=400, detail="Audio file must have a valid filename.")

    audio_bytes = await file.read()
    if len(audio_bytes) == 0:
        raise HTTPException(status_code=400, detail="Empty audio file provided.")

    try:
        # 1. Preprocess Audio (Mono, 16kHz, VAD)
        audio_array, audio_meta = preprocessor.load_from_bytes(audio_bytes)

        # 2. Layer 1: Deepfake / Synthetic Speech Detection
        deepfake_res = deepfake_detector.predict(audio_array)

        # 3. Layer 2: Prosodic & Behavioral Rhythm Dynamics
        prosody_res = prosody_analyzer.analyze(audio_array)

        # 4. Layer 3: Cross-Session Speaker Identity Verification
        speaker_res = None
        speaker_anomaly = None
        if claimed_speaker_id:
            speaker_res = speaker_engine.verify_speaker(claimed_speaker_id, audio_array)
            speaker_anomaly = speaker_res.get("identity_anomaly_score")

        # 5. Layer 4: Contextual & Composite Risk Engine
        context_data = {
            "transaction_amount": transaction_amount,
            "is_international": is_international,
            "caller_trust_score": 1.0,
            "urgency_flag": urgency_flag,
            "is_new_beneficiary": is_new_beneficiary
        }

        risk_evaluation = risk_engine.evaluate(
            synthetic_prob=deepfake_res["synthetic_probability"],
            acoustic_anomaly=deepfake_res["spectral_metrics"]["acoustic_anomaly_score"],
            prosodic_anomaly=prosody_res["prosodic_anomaly_score"],
            speaker_identity_anomaly=speaker_anomaly,
            context_metadata=context_data
        )

        # 6. Privacy & Zero-Retention
        session_id = PrivacyGuard.generate_anonymized_session_id(caller_id, datetime.utcnow().isoformat())
        compliance_log = PrivacyGuard.format_compliance_audit_log(
            session_id, risk_evaluation["overall_risk_score"], risk_evaluation["risk_tier"]
        )

        # Zero out buffer in RAM
        PrivacyGuard.purge_audio_buffer(audio_array)

        return AudioAnalysisResponse(
            session_id=session_id,
            overall_risk_score=risk_evaluation["overall_risk_score"],
            risk_tier=risk_evaluation["risk_tier"],
            action_required=risk_evaluation["action_required"],
            recommendations=risk_evaluation["recommendations"],
            classification=deepfake_res["classification"],
            synthetic_probability=deepfake_res["synthetic_probability"],
            bonafide_probability=deepfake_res["bonafide_probability"],
            spectral_biometrics=deepfake_res["spectral_metrics"],
            prosodic_dynamics=prosody_res,
            speaker_verification=speaker_res,
            context_threats=risk_evaluation["context_threats"],
            compliance=compliance_log
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Audio analysis error: {str(e)}")

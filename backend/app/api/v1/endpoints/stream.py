"""
Live WebSocket Audio Streaming & Real-Time Inspection
Owner: Person 3 (Backend) & Person 6 (Integration)
"""

from fastapi import APIRouter, WebSocket, WebSocketDisconnect
import json
import numpy as np

from speech.preprocessing.audio_loader import AudioPreprocessor
from ml.deepfake_detector.pipeline import DeepfakeDetector
from speech.prosody.analyzer import ProsodyAnalyzer
from security.risk_engine.evaluator import RiskScoringEngine
from integration.streaming.temporal_smoother import TemporalRiskSmoother

router = APIRouter()

preprocessor = AudioPreprocessor(target_sr=16000)
deepfake_detector = DeepfakeDetector()
prosody_analyzer = ProsodyAnalyzer(sample_rate=16000)
risk_engine = RiskScoringEngine()


@router.websocket("/live-stream")
async def live_audio_stream_endpoint(websocket: WebSocket):
    """
    Accepts continuous binary audio chunk streams over WebSocket,
    runs lightweight real-time feature extraction, and emits smoothed risk telemetry.
    """
    await websocket.accept()
    smoother = TemporalRiskSmoother(window_size=5, alpha=0.4)

    try:
        while True:
            # Receive audio chunk as binary bytes
            chunk_bytes = await websocket.receive_bytes()
            if len(chunk_bytes) < 1000:
                continue

            try:
                audio_array, _ = preprocessor.load_from_bytes(chunk_bytes)
            except Exception:
                # Raw float32 PCM fallback
                audio_array = np.frombuffer(chunk_bytes, dtype=np.float32)

            if len(audio_array) == 0:
                continue

            # Run detection on chunk
            deepfake_res = deepfake_detector.predict(audio_array)
            prosody_res = prosody_analyzer.analyze(audio_array)

            risk_eval = risk_engine.evaluate(
                synthetic_prob=deepfake_res["synthetic_probability"],
                acoustic_anomaly=deepfake_res["spectral_metrics"]["acoustic_anomaly_score"],
                prosodic_anomaly=prosody_res["prosodic_anomaly_score"]
            )

            # Apply temporal EMA smoothing
            smoothed_telemetry = smoother.update(risk_eval["overall_risk_score"])

            # Send real-time payload back to frontend HUD
            payload = {
                "event": "CHUNK_ANALYZED",
                "raw_risk": smoothed_telemetry["raw_chunk_score"],
                "smoothed_risk": smoothed_telemetry["smoothed_score"],
                "risk_tier": risk_eval["risk_tier"],
                "action": risk_eval["action_required"],
                "classification": deepfake_res["classification"],
                "synthetic_prob": deepfake_res["synthetic_probability"],
                "trend": smoothed_telemetry["trend"],
                "window_history": smoothed_telemetry["window_history"]
            }

            await websocket.send_text(json.dumps(payload))

    except WebSocketDisconnect:
        smoother.reset()
    except Exception as e:
        smoother.reset()
        await websocket.close()

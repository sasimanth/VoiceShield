# VoiceShield: Real-Time AI Voice Integrity & Impersonation Prevention Platform

**Smart India Hackathon 2026**  
**Problem Statement ID:** 26104  
**Title:** AI-Powered Real-Time Detection and Prevention of Voice Cloning Impersonation Attacks  

---

## 1. Team Structure & Module Ownership

The repository is modularly structured to allow all **6 team members** to work simultaneously in their dedicated directories without merge conflicts:

| Team Member | Role | Language & Tech | Primary Directory | Primary Deliverables |
| :--- | :--- | :--- | :--- | :--- |
| **Person 1** | **AI/ML Engineer** | 🐍 **Python** (PyTorch, AASIST) | [`ml/`](ml/) | Deepfake / synthetic speech detection, spectral features, model checkpoints. |
| **Person 2** | **Speech & Speaker Verification** | 🐍 **Python** (Librosa, Torchaudio) | [`speech/`](speech/) | VAD audio loader, prosody analysis (pitch $F_0$, jitter, shimmer), speaker embeddings. |
| **Person 3** | **Backend Engineer** | ☕ **Java** (Spring Boot 3, Maven) | [`backend/`](backend/) | REST controllers (`/audio/analyze`, `/speaker`, `/risk`), WebSocket streaming (`/ws/audio`), Swagger UI. |
| **Person 4** | **Risk & Cybersecurity Engineer** | ☕ **Java** (Maven, Jackson, JUnit 5) | [`risk_engine/`](risk_engine/) | Composite Multi-Signal Risk Engine, Contextual Fraud Scoring, Anti-Replay Nonces, DPDP Zero-Retention. |
| **Person 5** | **Frontend Engineer** | ⚛️ **TypeScript** (React + Vite) | [`frontend/`](frontend/) | SOC Cyber-Defense Dashboard, live audio waveform visualizer, threat alerts HUD. |
| **Person 6** | **Integration & QA Engineer** | 🐍 ☕ **Python & Java** (E2E Suites) | [`integration/`](integration/) | Frozen JSON contracts (`risk_contract.*`), E2E pipeline tests, sliding window temporal smoothing. |

---

## 2. Directory Architecture

```
voiceshield/
├── ml/                                 # [Person 1: AI/ML Engineer]
│   ├── deepfake_detector/
│   │   ├── model.py                    # SincNet + Graph Attention Network (AASIST)
│   │   ├── spectral_features.py        # Centroid, flatness, rolloff, high-frequency energy
│   │   └── pipeline.py                 # Prediction pipeline & calibration
│   └── checkpoints/                    # Pretrained model weights
│
├── speech/                             # [Person 2: Speech & Speaker Verification Engineer]
│   ├── preprocessing/
│   │   └── audio_loader.py             # 16kHz polyphase resampling, VAD silence trimming
│   ├── prosody/
│   │   └── analyzer.py                 # F0 pitch contour, jitter, shimmer, pause dynamics
│   └── speaker_verification/
│       └── verifier.py                 # Acoustic embeddings & cosine identity verification
│
├── backend/                            # [Person 3: Backend Engineer]
│   ├── app/
│   │   ├── api/v1/
│   │   │   ├── endpoints/
│   │   │   │   ├── audio.py            # POST /api/v1/audio/analyze
│   │   │   │   ├── speaker.py          # POST /api/v1/speaker/enroll & verify
│   │   │   │   └── stream.py           # WS /api/v1/ws/live-stream
│   │   │   └── router.py
│   │   ├── core/config.py              # Application settings
│   │   ├── schemas/audio.py            # Pydantic request/response schemas
│   │   └── main.py                     # FastAPI entrypoint
│   └── requirements.txt
│
├── security/                           # [Person 4: Risk & Cybersecurity Engineer]
│   ├── risk_engine/
│   │   └── evaluator.py                # Multi-Signal Composite Risk Score (0-100)
│   ├── context_engine/
│   │   └── context_analyzer.py         # Financial transaction amount & urgency evaluation
│   └── compliance/
│       └── privacy.py                  # DPDP Zero-Retention audio memory purge
│
├── frontend/                           # [Person 5: Frontend Engineer]
│   ├── src/
│   │   ├── types/index.ts              # TypeScript interfaces
│   │   ├── App.tsx                     # SOC Cyber-Defense Dashboard
│   │   ├── main.tsx
│   │   └── index.css                   # Tailwind styles
│   ├── package.json
│   └── vite.config.ts
│
├── integration/                        # [Person 6: Real-Time Integration & QA Engineer]
│   ├── streaming/
│   │   └── temporal_smoother.py        # Sliding window EMA risk smoothing
│   ├── tests/
│   │   └── test_e2e_pipeline.py        # E2E Pytest integration test suite
│   └── benchmarks/
│       └── benchmark_suite.py          # EER, ROC-AUC calculation harness
│
├── .gitignore
├── .env.example
└── README.md
```

---

## 3. Quickstart & Local Setup

### Backend Setup (Python 3.10+)
```bash
# 1. Create and activate virtual environment
python -m venv .venv
# On Windows:
.venv\Scripts\activate
# On Linux/macOS:
source .venv/bin/activate

# 2. Install dependencies
pip install -r backend/requirements.txt

# 3. Configure environment
cp .env.example .env

# 4. Start backend server
uvicorn backend.app.main:app --reload --port 8000
```
> **Swagger API Documentation:** `http://localhost:8000/docs`

### Frontend Setup (Node.js 18+)
```bash
cd frontend
npm install
npm run dev
```
> **Frontend Dashboard:** `http://localhost:5173`

### Running Integration Tests
```bash
pytest integration/tests/
```

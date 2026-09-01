# VoiceShield: Real-Time AI Voice Integrity & Impersonation Prevention Platform

**Smart India Hackathon 2026**  
**Problem Statement ID:** 26104  
**Title:** AI-Powered Real-Time Detection and Prevention of Voice Cloning Impersonation Attacks  

---

## 1. Team Structure & Module Ownership

The repository is modularly structured to allow all **6 team members** to work simultaneously in their dedicated directories without merge conflicts:

| Team Member | Role | Primary Directory | Primary Ownership & Deliverables |
| :--- | :--- | :--- | :--- |
| **Person 1** | **AI/ML Engineer** | [`ml/`](ml/) | Deepfake / synthetic speech detection, AASIST neural model, spectral feature extraction, model weights & checkpoint training. |
| **Person 2** | **Speech & Speaker Verification Engineer** | [`speech/`](speech/) | Audio ingestion & VAD preprocessing, Prosody analysis (pitch $F_0$, jitter, shimmer, rhythm), and Speaker Verification (embedding similarity against enrolled genuine voice profile). |
| **Person 3** | **Backend Engineer** | [`backend/`](backend/) | FastAPI application, REST endpoints (`/audio/analyze`, `/speaker/enroll`, `/speaker/verify`), database schemas, and service orchestration. |
| **Person 4** | **Risk & Cybersecurity Engineer** | [`security/`](security/) | Composite Multi-Signal Risk Engine, Contextual Banking Threat Analysis, JWT authentication, and DPDP Zero-Retention Privacy compliance. |
| **Person 5** | **Frontend Engineer** | [`frontend/`](frontend/) | React 19 + Vite + Tailwind CSS Cyber-Defense SOC Dashboard, real-time waveform visualizers, pre-transaction alert prompts, and executive escalation HUD. |
| **Person 6** | **Real-Time Integration & QA Engineer** | [`integration/`](integration/) | Real-Time WebSocket streaming (`/ws/live-stream`), temporal EMA risk smoothing, E2E integration test suite, and EER/ROC-AUC benchmark evaluations. |

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

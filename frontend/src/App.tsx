import React, { useEffect, useMemo, useState } from "react";
import {
  Activity,
  AlertTriangle,
  AudioLines,
  CheckCircle2,
  ChevronRight,
  Clock3,
  FileAudio,
  Fingerprint,
  Gauge,
  Headphones,
  Lock,
  Menu,
  Mic,
  MicOff,
  Radio,
  RefreshCw,
  Shield,
  ShieldAlert,
  ShieldCheck,
  Siren,
  Upload,
  UserRound,
  Waves,
  Wifi,
  XCircle,
  Zap,
} from "lucide-react";

import type { AnalysisResult } from "./types/analysis";
import { analyzeAudio } from "./services/api";
import { liveStreamService, StreamTelemetry } from "./services/liveStreamService";

const MOCK_RESULT: AnalysisResult = {
  session_id: "VS-1042",
  timestamp: new Date().toISOString(),
  synthetic_probability: 0.82,
  similarity_score: 0.31,
  context_risk: "HIGH",
  risk_score: 91,
  risk_level: "CRITICAL",
  decision: "SECONDARY_VERIFICATION",
  reasons: [
    "Elevated synthetic speech score detected",
    "Speaker mismatch against enrolled biometric profile",
    "High-value transaction combined with urgency indicators",
  ],
  recommended_action: "mfa_and_callback",
};

type View = "overview" | "analysis" | "live" | "alerts" | "differentiators";

type VerificationState =
  | "NOT_REQUIRED"
  | "PENDING"
  | "VERIFIED"
  | "REJECTED";

function riskBadgeClass(level: string) {
  switch (level.toUpperCase()) {
    case "CRITICAL":
      return "risk-critical";

    case "HIGH":
      return "risk-high";

    case "MEDIUM":
      return "risk-medium";

    default:
      return "risk-low";
  }
}

function formatAction(value: string) {
  return value
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (char) => char.toUpperCase());
}

function formatTime(timestamp: string) {
  return new Date(timestamp).toLocaleString();
}

function scoreWidth(score: number) {
  return Math.min(100, Math.max(0, score));
}

export default function App() {
  const [activeView, setActiveView] = useState<View>("overview");
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<AnalysisResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [sessionId, setSessionId] = useState("VS-1042");
  const [claimedSpeaker, setClaimedSpeaker] = useState("");
  const [transactionAmount, setTransactionAmount] = useState("50000");
  const [urgencyFlag, setUrgencyFlag] = useState(false);
  const [isNewBeneficiary, setIsNewBeneficiary] = useState(false);

  const [useMockMode, setUseMockMode] = useState(false);

  const [liveConnected, setLiveConnected] = useState(false);
  const [liveRisk, setLiveRisk] = useState(12);
  const [liveTier, setLiveTier] = useState("LOW");
  const [streamMode, setStreamMode] = useState<"idle" | "mic" | "simulation">("idle");
  const [streamLogs, setStreamLogs] = useState<StreamTelemetry[]>([]);
  const [frequencyData, setFrequencyData] = useState<Uint8Array>(new Uint8Array(32));
  const [streamLatency, setStreamLatency] = useState<number>(78);
  const [streamDecision, setStreamDecision] = useState<string>("ALLOW");
  const [streamReasons, setStreamReasons] = useState<string[]>([
    "Channel idle - awaiting stream initiation",
  ]);

  const [verificationState, setVerificationState] =
    useState<VerificationState>("NOT_REQUIRED");

  const [alerts, setAlerts] = useState<string[]>([
    "No unresolved security alerts",
  ]);

  useEffect(() => {
    document.title = "VoiceShield — Real-Time AI Voice Clone Detection";
    return () => {
      liveStreamService.stop();
    };
  }, []);

  const loadPresetSample = async (url: string, filename: string) => {
    try {
      setError(null);
      setLoading(true);
      const res = await fetch(url);
      const blob = await res.blob();
      const sampleFile = new File([blob], filename, { type: "audio/wav" });
      setFile(sampleFile);
    } catch (e) {
      console.error("Failed to load sample:", e);
      setError("Failed to load preset sample. Please try uploading manually.");
    } finally {
      setLoading(false);
    }
  };

  const handleStartMicStream = async () => {
    setStreamMode("mic");
    setLiveConnected(true);
    await liveStreamService.startMicrophone(
      (data) => {
        setLiveRisk(data.riskScore);
        setLiveTier(data.riskLevel);
        setStreamDecision(data.decision);
        setStreamReasons(data.reasons);
        setStreamLatency(data.latencyMs);
        setStreamLogs((prev) => [data, ...prev.slice(0, 19)]);
      },
      (freqs) => {
        setFrequencyData(new Uint8Array(freqs));
      }
    );
  };

  const handleStartSimulationStream = () => {
    setStreamMode("simulation");
    setLiveConnected(true);
    liveStreamService.startSimulation(
      (data) => {
        setLiveRisk(data.riskScore);
        setLiveTier(data.riskLevel);
        setStreamDecision(data.decision);
        setStreamReasons(data.reasons);
        setStreamLatency(data.latencyMs);
        setStreamLogs((prev) => [data, ...prev.slice(0, 19)]);
      },
      (freqs) => {
        setFrequencyData(new Uint8Array(freqs));
      }
    );
  };

  const handleStopStream = () => {
    liveStreamService.stop();
    setStreamMode("idle");
    setLiveConnected(false);
    setFrequencyData(new Uint8Array(32));
  };

  const currentRisk = result?.risk_score ?? 0;
  const currentTier = result?.risk_level ?? "LOW";

  const syntheticPercent = useMemo(
    () => Math.round((result?.synthetic_probability ?? 0) * 100),
    [result]
  );

  const speakerPercent = useMemo(
    () => Math.round((result?.similarity_score ?? 0) * 100),
    [result]
  );

  const handleAnalyze = async (
    event: React.FormEvent<HTMLFormElement>
  ) => {
    event.preventDefault();

    if (!file) {
      setError("Select an audio file before starting analysis.");
      return;
    }

    const value = Number(transactionAmount);

    if (!Number.isFinite(value) || value < 0) {
      setError("Enter a valid transaction amount.");
      return;
    }

    setError(null);
    setLoading(true);
    setVerificationState("NOT_REQUIRED");

    try {
      if (useMockMode) {
        await new Promise((resolve) => setTimeout(resolve, 900));

        const mock: AnalysisResult = {
          ...MOCK_RESULT,
          session_id: sessionId || "VS-1042",
          timestamp: new Date().toISOString(),
        };

        setResult(mock);

        if (
          mock.risk_level === "HIGH" ||
          mock.risk_level === "CRITICAL"
        ) {
          setVerificationState("PENDING");
          setAlerts((current) => [
            "High-risk voice integrity event detected",
            ...current.filter(
              (item) =>
                item !== "No unresolved security alerts"
            ),
          ]);
        }
      } else {
        const response = await analyzeAudio({
          file,
          sessionId,
          claimedSpeakerId: claimedSpeaker,
          transactionValueInr: value,
          urgentSocialEngineering: urgencyFlag,
          unverifiedBeneficiary: isNewBeneficiary,
        });

        setResult(response);

        if (
          response.risk_level === "HIGH" ||
          response.risk_level === "CRITICAL"
        ) {
          setVerificationState("PENDING");
        }
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Voice analysis failed."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleVerification = () => {
    setVerificationState("VERIFIED");
  };

  const handleReject = () => {
    setVerificationState("REJECTED");
  };

  const resetAnalysis = () => {
    setResult(null);
    setError(null);
    setVerificationState("NOT_REQUIRED");
  };

  return (
    <div className="app-shell">
      {/* TOP HEADER */}
      <header className="topbar">
        <div className="brand-area">
          <button
            className="mobile-menu-button"
            onClick={() =>
              setMobileMenuOpen((current) => !current)
            }
            aria-label="Toggle navigation"
          >
            <Menu size={20} />
          </button>

          <div className="brand-mark">
            <Shield size={21} />
          </div>

          <div>
            <div className="brand-title-row">
              <h1>VoiceShield</h1>
              <span className="brand-tag">SIH 2026</span>
            </div>

            <p>
              AI Real-Time Voice Integrity & Impersonation Prevention
            </p>
          </div>
        </div>

        <div className="topbar-status">
          <div className="status-pill">
            <span className="status-dot live" />
            SYSTEM OPERATIONAL
          </div>

          <div className="status-pill">
            <Lock size={13} />
            DPDP ZERO-RETENTION
          </div>

          <div className="user-pill">
            <div className="user-avatar">
              <UserRound size={15} />
            </div>
            <div>
              <strong>Security Analyst</strong>
              <span>Operations Console</span>
            </div>
          </div>
        </div>
      </header>

      <div className="workspace">
        {/* SIDEBAR */}
        <aside
          className={`sidebar ${
            mobileMenuOpen ? "sidebar-open" : ""
          }`}
        >
          <div className="sidebar-section">
            <span className="sidebar-label">MONITORING</span>

            <button
              className={`nav-item ${
                activeView === "overview" ? "active" : ""
              }`}
              onClick={() => {
                setActiveView("overview");
                setMobileMenuOpen(false);
              }}
            >
              <Gauge size={17} />
              <span>Security Overview</span>
            </button>

            <button
              className={`nav-item ${
                activeView === "analysis" ? "active" : ""
              }`}
              onClick={() => {
                setActiveView("analysis");
                setMobileMenuOpen(false);
              }}
            >
              <FileAudio size={17} />
              <span>Audio Analysis</span>
            </button>

            <button
              className={`nav-item ${
                activeView === "live" ? "active" : ""
              }`}
              onClick={() => {
                setActiveView("live");
                setMobileMenuOpen(false);
              }}
            >
              <Radio size={17} />
              <span>Live Stream</span>
            </button>

            <button
              className={`nav-item ${
                activeView === "alerts" ? "active" : ""
              }`}
              onClick={() => {
                setActiveView("alerts");
                setMobileMenuOpen(false);
              }}
            >
              <Siren size={17} />
              <span>Security Alerts</span>
              <span className="nav-count">
                {alerts.filter(
                  (alert) =>
                    alert !==
                    "No unresolved security alerts"
                ).length}
              </span>
            </button>

            <button
              className={`nav-item ${
                activeView === "differentiators" ? "active" : ""
              }`}
              onClick={() => {
                setActiveView("differentiators");
                setMobileMenuOpen(false);
              }}
            >
              <Zap size={17} />
              <span>SIH Competitive Edge</span>
              <span className="nav-badge-usp">USP</span>
            </button>
          </div>

          <div className="sidebar-divider" />

          <div className="sidebar-section">
            <span className="sidebar-label">PLATFORM</span>

            <div className="sidebar-info">
              <div className="info-icon">
                <ServerIcon />
              </div>
              <div>
                <strong>Analysis Engine</strong>
                <span>Connected interface</span>
              </div>
            </div>

            <div className="sidebar-info">
              <div className="info-icon green">
                <Wifi size={15} />
              </div>
              <div>
                <strong>Realtime Channel</strong>
                <span>
                  {liveConnected
                    ? "Connected"
                    : "Standby"}
                </span>
              </div>
            </div>
          </div>

          <div className="sidebar-footer">
            <span>Problem Statement</span>
            <strong>PSC26104</strong>
          </div>
        </aside>

        {/* MAIN WORKSPACE */}
        <main className="main-area">
          <div className="page-heading">
            <div>
              <span className="eyebrow">
                SECURITY OPERATIONS CENTER
              </span>
              <h2>
                {activeView === "overview" &&
                  "Voice Integrity Overview"}
                {activeView === "analysis" &&
                  "Forensic Audio Analysis"}
                {activeView === "live" &&
                  "Live Voice Integrity Monitor"}
                {activeView === "alerts" &&
                  "Security Alerts"}
              </h2>
              <p>
                Monitor voice authenticity, impersonation
                indicators and verification decisions.
              </p>
            </div>

            <div className="page-actions">
              <div className="mode-toggle">
                <span>Prototype Mode</span>
                <button
                  className={`switch ${
                    useMockMode ? "on" : ""
                  }`}
                  onClick={() =>
                    setUseMockMode(
                      (current) => !current
                    )
                  }
                  aria-label="Toggle prototype mode"
                >
                  <span />
                </button>
              </div>

              <button
                className="icon-button"
                onClick={resetAnalysis}
                title="Reset analysis"
              >
                <RefreshCw size={16} />
              </button>
            </div>
          </div>

          {/* OVERVIEW */}
          {activeView === "overview" && (
            <>
              <section className="metric-grid">
                <div className="metric-card">
                  <div className="metric-card-top">
                    <span>ACTIVE SESSION</span>
                    <Activity size={17} />
                  </div>
                  <strong>
                    {result?.session_id ?? "No active session"}
                  </strong>
                  <small>
                    {result
                      ? "Analysis available"
                      : "Awaiting audio submission"}
                  </small>
                </div>

                <div className="metric-card">
                  <div className="metric-card-top">
                    <span>SYNTHETIC RISK</span>
                    <Waves size={17} />
                  </div>
                  <strong>
                    {result ? `${syntheticPercent}%` : "--"}
                  </strong>
                  <small>
                    {result
                      ? "Synthetic probability"
                      : "No analysis yet"}
                  </small>
                </div>

                <div className="metric-card">
                  <div className="metric-card-top">
                    <span>SPEAKER MATCH</span>
                    <Fingerprint size={17} />
                  </div>
                  <strong>
                    {result ? `${speakerPercent}%` : "--"}
                  </strong>
                  <small>
                    Similarity score
                  </small>
                </div>

                <div className="metric-card">
                  <div className="metric-card-top">
                    <span>OVERALL RISK</span>
                    <ShieldAlert size={17} />
                  </div>
                  <strong>
                    {result ? result.risk_score : "--"}
                  </strong>
                  <small>
                    {result
                      ? result.risk_level
                      : "No active threat"}
                  </small>
                </div>
              </section>

              <section className="dashboard-grid">
                <div className="panel risk-panel">
                  <div className="panel-header">
                    <div>
                      <span className="panel-kicker">
                        CURRENT ASSESSMENT
                      </span>
                      <h3>Threat Assessment</h3>
                    </div>

                    {result ? (
                      <span
                        className={`risk-badge ${riskBadgeClass(
                          currentTier
                        )}`}
                      >
                        {currentTier}
                      </span>
                    ) : (
                      <span className="neutral-badge">
                        NO DATA
                      </span>
                    )}
                  </div>

                  <div className="risk-center">
                    <div className="risk-ring">
                      <div className="risk-ring-inner">
                        <span>
                          {result ? currentRisk : "--"}
                        </span>
                        <small>/ 100</small>
                      </div>
                    </div>

                    <div className="risk-description">
                      <span className="risk-caption">
                        OVERALL RISK SCORE
                      </span>

                      <h4>
                        {result
                          ? `${currentTier} RISK`
                          : "AWAITING ANALYSIS"}
                      </h4>

                      <p>
                        {result
                          ? "The current assessment combines voice authenticity indicators, speaker similarity and transaction context."
                          : "Submit an audio sample to generate a multi-layer voice integrity assessment."}
                      </p>
                    </div>
                  </div>

                  <div className="risk-breakdown">
                    <RiskBar
                      label="Synthetic Speech"
                      value={syntheticPercent}
                    />

                    <RiskBar
                      label="Speaker Similarity"
                      value={speakerPercent}
                      inverse
                    />

                    <RiskBar
                      label="Transaction Context"
                      value={
                        result
                          ? result.context_risk.toUpperCase() ===
                            "CRITICAL"
                            ? 100
                            : result.context_risk.toUpperCase() ===
                                "HIGH"
                              ? 80
                              : result.context_risk.toUpperCase() ===
                                  "MEDIUM"
                                ? 55
                                : 25
                          : 0
                      }
                    />
                  </div>
                </div>

                <div className="panel session-panel">
                  <div className="panel-header">
                    <div>
                      <span className="panel-kicker">
                        ACTIVE CASE
                      </span>
                      <h3>Session Details</h3>
                    </div>
                    <Clock3 size={17} />
                  </div>

                  <div className="session-list">
                    <DetailRow
                      label="Session ID"
                      value={
                        result?.session_id ?? "VS-1042"
                      }
                    />

                    <DetailRow
                      label="Claimed Speaker"
                      value={claimedSpeaker}
                    />

                    <DetailRow
                      label="Transaction Value"
                      value={`₹${Number(
                        transactionAmount || 0
                      ).toLocaleString("en-IN")}`}
                    />

                    <DetailRow
                      label="Context Risk"
                      value={
                        result
                          ? formatAction(
                              result.context_risk
                            )
                          : "Not evaluated"
                      }
                    />

                    <DetailRow
                      label="Analysis Timestamp"
                      value={
                        result
                          ? formatTime(result.timestamp)
                          : "—"
                      }
                    />
                  </div>

                  <button
                    className="primary-button full-width"
                    onClick={() =>
                      setActiveView("analysis")
                    }
                  >
                    <AudioLines size={16} />
                    Open Audio Analysis
                    <ChevronRight size={16} />
                  </button>
                </div>
              </section>

              <section className="bottom-grid">
                <div className="panel">
                  <div className="panel-header">
                    <div>
                      <span className="panel-kicker">
                        ANALYSIS EXPLANATION
                      </span>
                      <h3>Why the System Raised Risk</h3>
                    </div>
                  </div>

                  {result ? (
                    <div className="reason-list">
                      {result.reasons.map(
                        (reason, index) => (
                          <div
                            className="reason-row"
                            key={`${reason}-${index}`}
                          >
                            <div className="reason-number">
                              {String(index + 1).padStart(
                                2,
                                "0"
                              )}
                            </div>

                            <div>
                              <strong>{reason}</strong>
                              <span>
                                Security indicator returned
                                by the analysis pipeline.
                              </span>
                            </div>
                          </div>
                        )
                      )}
                    </div>
                  ) : (
                    <EmptyState
                      icon={<Activity size={18} />}
                      title="No assessment available"
                      text="Run an audio analysis to view the factors contributing to the risk score."
                    />
                  )}
                </div>

                <div className="panel action-panel">
                  <div className="panel-header">
                    <div>
                      <span className="panel-kicker">
                        RESPONSE
                      </span>
                      <h3>Recommended Action</h3>
                    </div>
                  </div>

                  {result ? (
                    <>
                      <div className="recommended-action">
                        <div className="action-icon">
                          <Zap size={19} />
                        </div>

                        <div>
                          <strong>
                            {formatAction(
                              result.recommended_action
                            )}
                          </strong>

                          <span>
                            Decision state:{" "}
                            {formatAction(
                              result.decision
                            )}
                          </span>
                        </div>
                      </div>

                      <button
                        className="primary-button"
                        onClick={() => {
                          setActiveView("analysis");
                          setVerificationState(
                            "PENDING"
                          );
                        }}
                      >
                        Start Verification
                        <ChevronRight size={16} />
                      </button>
                    </>
                  ) : (
                    <EmptyState
                      icon={<ShieldCheck size={18} />}
                      title="No action required yet"
                      text="A recommended response will appear after analysis."
                    />
                  )}
                </div>
              </section>
            </>
          )}

          {/* AUDIO ANALYSIS */}
          {activeView === "analysis" && (
            <section className="analysis-layout">
              <div className="panel upload-panel">
                <div className="panel-header">
                  <div>
                    <span className="panel-kicker">
                      FORENSIC INPUT
                    </span>
                    <h3>Audio & Transaction Context</h3>
                  </div>
                  <FileAudio size={18} />
                </div>

                <form
                  className="analysis-form"
                  onSubmit={handleAnalyze}
                >
                  <div className="preset-samples-wrapper">
                    <span className="preset-title">SIH DEMO TEST PRESETS (1-CLICK LOAD):</span>
                    <div className="preset-buttons-row">
                      <button
                        type="button"
                        className="preset-btn preset-btn-human"
                        onClick={() =>
                          loadPresetSample(
                            "/samples/human_voice_sample.wav",
                            "genuine_human_voice.wav"
                          )
                        }
                      >
                        <CheckCircle2 size={15} />
                        <span>🟢 Genuine Human Voice</span>
                      </button>
                      <button
                        type="button"
                        className="preset-btn preset-btn-deepfake"
                        onClick={() =>
                          loadPresetSample(
                            "/samples/deepfake_clone_sample.wav",
                            "ai_voice_clone_scam.wav"
                          )
                        }
                      >
                        <AlertTriangle size={15} />
                        <span>🔴 AI Voice Clone Scam</span>
                      </button>
                    </div>
                  </div>

                  <div className="field">
                    <label htmlFor="audio">
                      Audio File
                    </label>

                    <div className="upload-zone">
                      <Upload size={20} />

                      <strong>
                        Select audio evidence
                      </strong>

                      <span>
                        Supported: WAV, MP3, FLAC
                      </span>

                      <input
                        id="audio"
                        type="file"
                        accept=".wav,.mp3,.flac,audio/wav,audio/mpeg,audio/flac"
                        onChange={(event) =>
                          setFile(
                            event.target.files?.[0] ??
                              null
                          )
                        }
                      />

                      {file && (
                        <div className="selected-file">
                          <FileAudio size={15} />
                          <span>{file.name}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="form-two-columns">
                    <div className="field">
                      <label htmlFor="session">
                        Session ID
                      </label>
                      <input
                        id="session"
                        value={sessionId}
                        onChange={(event) =>
                          setSessionId(
                            event.target.value
                          )
                        }
                        placeholder="VS-1042"
                      />
                    </div>

                    <div className="field">
                      <label htmlFor="speaker">
                        Claimed Speaker ID
                      </label>
                      <input
                        id="speaker"
                        value={claimedSpeaker}
                        onChange={(event) =>
                          setClaimedSpeaker(
                            event.target.value
                          )
                        }
                        placeholder="VIP-CEO-01"
                      />
                    </div>
                  </div>

                  <div className="field">
                    <label htmlFor="amount">
                      Requested Transaction Value (INR)
                    </label>
                    <div className="currency-input">
                      <span>₹</span>
                      <input
                        id="amount"
                        type="number"
                        min="0"
                        value={transactionAmount}
                        onChange={(event) =>
                          setTransactionAmount(
                            event.target.value
                          )
                        }
                      />
                    </div>
                  </div>

                  <div className="toggle-card">
                    <label>
                      <span>
                        <strong>
                          Urgent Social Engineering
                        </strong>
                        <small>
                          Caller pressure / urgency
                          indicator
                        </small>
                      </span>
                      <input
                        type="checkbox"
                        checked={urgencyFlag}
                        onChange={(event) =>
                          setUrgencyFlag(
                            event.target.checked
                          )
                        }
                      />
                    </label>

                    <label>
                      <span>
                        <strong>
                          Unverified Beneficiary
                        </strong>
                        <small>
                          New or untrusted transaction
                          destination
                        </small>
                      </span>
                      <input
                        type="checkbox"
                        checked={isNewBeneficiary}
                        onChange={(event) =>
                          setIsNewBeneficiary(
                            event.target.checked
                          )
                        }
                      />
                    </label>
                  </div>

                  {error && (
                    <div className="error-banner">
                      <AlertTriangle size={16} />
                      <span>{error}</span>
                    </div>
                  )}

                  <button
                    type="submit"
                    className="primary-button full-width analyze-button"
                    disabled={loading}
                  >
                    {loading ? (
                      <>
                        <RefreshCw
                          size={17}
                          className="spin"
                        />
                        Analyzing Audio...
                      </>
                    ) : (
                      <>
                        <ShieldCheck size={17} />
                        Inspect Voice Authenticity
                      </>
                    )}
                  </button>
                </form>
              </div>

              <div className="analysis-results">
                {!result ? (
                  <div className="panel empty-analysis">
                    <div className="empty-analysis-icon">
                      <Headphones size={27} />
                    </div>

                    <span className="panel-kicker">
                      ANALYSIS CONSOLE
                    </span>

                    <h3>Awaiting Audio Submission</h3>

                    <p>
                      Upload a voice sample on the left to
                      generate the security assessment.
                    </p>

                    <div className="process-flow">
                      <ProcessStep
                        number="01"
                        label="Audio"
                      />
                      <ProcessLine />
                      <ProcessStep
                        number="02"
                        label="Detection"
                      />
                      <ProcessLine />
                      <ProcessStep
                        number="03"
                        label="Risk"
                      />
                      <ProcessLine />
                      <ProcessStep
                        number="04"
                        label="Response"
                      />
                    </div>
                  </div>
                ) : (
                  <>
                    <div
                      className={`panel result-banner ${riskBadgeClass(
                        result.risk_level
                      )}`}
                    >
                      <div className="result-banner-icon">
                        {result.risk_level ===
                        "CRITICAL" ||
                        result.risk_level === "HIGH" ? (
                          <ShieldAlert size={24} />
                        ) : (
                          <ShieldCheck size={24} />
                        )}
                      </div>

                      <div className="result-banner-copy">
                        <span>
                          VOICE INTEGRITY ASSESSMENT
                        </span>

                        <h3>
                          {result.risk_level} RISK
                        </h3>

                        <p>
                          Elevated likelihood of
                          synthetic/manipulated speech
                          detected.
                        </p>
                      </div>

                      <div className="result-score">
                        <strong>
                          {result.risk_score}
                        </strong>
                        <span>RISK SCORE</span>
                      </div>
                    </div>

                    <div className="analysis-stat-grid">
                      <AnalysisStat
                        label="Synthetic Probability"
                        value={`${syntheticPercent}%`}
                        progress={syntheticPercent}
                        icon={<Waves size={17} />}
                      />

                      <AnalysisStat
                        label="Speaker Similarity"
                        value={`${speakerPercent}%`}
                        progress={speakerPercent}
                        icon={<Fingerprint size={17} />}
                      />

                      <AnalysisStat
                        label="Context Risk"
                        value={result.context_risk}
                        text
                        icon={<AlertTriangle size={17} />}
                      />
                    </div>

                    <div className="panel">
                      <div className="panel-header">
                        <div>
                          <span className="panel-kicker">
                            DECISION INTELLIGENCE
                          </span>
                          <h3>Risk Factors</h3>
                        </div>
                      </div>

                      <div className="reason-list">
                        {result.reasons.map(
                          (reason, index) => (
                            <div
                              className="reason-row"
                              key={`${reason}-${index}`}
                            >
                              <div className="reason-number">
                                {String(index + 1).padStart(
                                  2,
                                  "0"
                                )}
                              </div>

                              <div>
                                <strong>{reason}</strong>
                                <span>
                                  Evidence contributing to
                                  the current risk decision.
                                </span>
                              </div>
                            </div>
                          )
                        )}
                      </div>
                    </div>

                    <div className="panel verification-panel">
                      <div className="panel-header">
                        <div>
                          <span className="panel-kicker">
                            SECURITY RESPONSE
                          </span>
                          <h3>
                            Secondary Verification
                          </h3>
                        </div>

                        <span
                          className={`verification-badge ${verificationState.toLowerCase()}`}
                        >
                          {verificationState.replace(
                            /_/g,
                            " "
                          )}
                        </span>
                      </div>

                      <p className="verification-copy">
                        Recommended action:{" "}
                        <strong>
                          {formatAction(
                            result.recommended_action
                          )}
                        </strong>
                      </p>

                      <div className="verification-actions">
                        <button
                          className="primary-button"
                          onClick={handleVerification}
                        >
                          <CheckCircle2 size={16} />
                          Mark Verified
                        </button>

                        <button
                          className="secondary-button danger"
                          onClick={handleReject}
                        >
                          <XCircle size={16} />
                          Reject / Escalate
                        </button>
                      </div>
                    </div>
                  </>
                )}
              </div>
            </section>
          )}

          {/* LIVE STREAM */}
          {activeView === "live" && (
            <section className="live-layout">
              <div className="panel live-header-panel">
                <div>
                  <span className="panel-kicker">
                    SUB-150MS TELEPHONY DEFENSE
                  </span>
                  <h3>Real-Time Live Call Stream & Oscilloscope</h3>
                  <p>
                    Inspect in-call voice biometrics in real-time over Spring Boot WebSocket (`ws://localhost:8080/ws/audio`).
                  </p>
                </div>

                <div className="live-controls-group">
                  <span
                    className={`connection-status ${
                      liveConnected ? "connected" : ""
                    }`}
                  >
                    <span />
                    {liveConnected
                      ? streamMode === "mic"
                        ? "LIVE MIC ACTIVE"
                        : "TELEPHONY SIMULATOR ACTIVE"
                      : "STANDBY"}
                  </span>

                  {!liveConnected ? (
                    <div className="stream-action-buttons">
                      <button
                        type="button"
                        className="primary-button stream-btn"
                        onClick={handleStartMicStream}
                      >
                        <Mic size={16} />
                        Stream Live Mic
                      </button>
                      <button
                        type="button"
                        className="secondary-button stream-btn"
                        onClick={handleStartSimulationStream}
                      >
                        <Radio size={16} />
                        Simulate Telephony Call
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      className="secondary-button danger stream-btn"
                      onClick={handleStopStream}
                    >
                      <MicOff size={16} />
                      Stop Stream
                    </button>
                  )}
                </div>
              </div>

              <div className="metric-grid live-metrics">
                <div className="metric-card large">
                  <div className="metric-card-top">
                    <span>ROLLING RISK SCORE</span>
                    <Activity size={17} />
                  </div>
                  <strong className={liveRisk > 70 ? "text-danger" : liveRisk > 35 ? "text-warning" : "text-success"}>
                    {liveConnected ? `${liveRisk}/100` : "--"}
                  </strong>
                  <small>Sliding window composite score</small>
                </div>

                <div className="metric-card large">
                  <div className="metric-card-top">
                    <span>LIVE DECISION TIER</span>
                    <ShieldAlert size={17} />
                  </div>
                  <strong className={streamDecision === "BLOCK" ? "text-danger" : streamDecision === "SECONDARY_VERIFICATION" ? "text-warning" : "text-success"}>
                    {liveConnected ? `${streamDecision} • ${liveTier}` : "STANDBY"}
                  </strong>
                  <small>{liveConnected && streamReasons[0] ? streamReasons[0].slice(0, 45) + "..." : "Automated fraud policy tier"}</small>
                </div>

                <div className="metric-card large">
                  <div className="metric-card-top">
                    <span>STREAM LATENCY</span>
                    <Zap size={17} />
                  </div>
                  <strong className="text-cyan">
                    {liveConnected ? `${streamLatency} ms` : "--"}
                  </strong>
                  <small>Sub-150ms in-call guarantee</small>
                </div>

                <div className="metric-card large">
                  <div className="metric-card-top">
                    <span>PRIVACY GUARANTEE</span>
                    <Lock size={17} />
                  </div>
                  <strong className="text-emerald">
                    {liveConnected ? "DPDP COMPLIANT" : "ZERO-RETENTION"}
                  </strong>
                  <small>Stateless RAM inference</small>
                </div>
              </div>

              {/* Dynamic Oscilloscope & Frequency Spectrum Visualizer */}
              <div className="panel waveform-panel">
                <div className="panel-header">
                  <div>
                    <span className="panel-kicker">LIVE OSCILLOSCOPE</span>
                    <h3>Acoustic Harmonic Frequency Spectrum (16 kHz)</h3>
                  </div>

                  <span className={`live-indicator ${liveConnected ? "pulsing" : ""}`}>
                    <span />
                    {liveConnected ? "STREAMING" : "IDLE"}
                  </span>
                </div>

                <div className="waveform live-frequency-bars">
                  {Array.from({ length: 32 }).map((_, index) => {
                    const rawVal = frequencyData[index] || 0;
                    const barHeight = liveConnected
                      ? Math.max(12, Math.min(85, Math.floor((rawVal / 255) * 80) + 12))
                      : 8;
                    const isHighRiskFreq = index > 20 && liveRisk > 50;

                    return (
                      <div key={index} className="freq-bar-wrapper">
                        <span
                          className={`freq-bar ${isHighRiskFreq ? "freq-bar-alert" : ""}`}
                          style={{ height: `${barHeight}px` }}
                        />
                      </div>
                    );
                  })}
                </div>

                <div className="waveform-footer">
                  <span>80 Hz (Pitch F0)</span>
                  <span>1.2 kHz (Formants F1/F2)</span>
                  <span>4.0 kHz (Vocoder Cutoff)</span>
                  <span>8.0 kHz (Nyquist HF)</span>
                </div>
              </div>

              {/* Live Streaming Log Table */}
              <div className="panel">
                <div className="panel-header">
                  <div>
                    <span className="panel-kicker">IN-CALL TELEMETRY LOG</span>
                    <h3>Streaming Chunks & Fraud Interception Audit</h3>
                  </div>
                  <span className="log-badge">{streamLogs.length} Packets Evaluated</span>
                </div>

                {streamLogs.length === 0 ? (
                  <div className="empty-stream-box">
                    <Radio size={24} />
                    <p>Click "Stream Live Mic" or "Simulate Telephony Call" to view real-time WebSocket packet evaluation.</p>
                  </div>
                ) : (
                  <div className="stream-table-wrapper">
                    <table className="stream-table">
                      <thead>
                        <tr>
                          <th>CHUNK #</th>
                          <th>TIMESTAMP</th>
                          <th>PAYLOAD</th>
                          <th>LATENCY</th>
                          <th>RISK SCORE</th>
                          <th>DECISION</th>
                          <th>REAL-TIME REASON CODES</th>
                        </tr>
                      </thead>
                      <tbody>
                        {streamLogs.map((log) => (
                          <tr key={log.chunkIndex} className={log.decision === "BLOCK" ? "row-blocked" : ""}>
                            <td>#{log.chunkIndex}</td>
                            <td>{log.timestamp}</td>
                            <td>{log.chunkBytes} bytes</td>
                            <td>{log.latencyMs} ms</td>
                            <td>
                              <span className={`score-badge ${log.riskScore > 60 ? "badge-danger" : "badge-safe"}`}>
                                {log.riskScore}/100
                              </span>
                            </td>
                            <td>
                              <span className={`decision-badge ${log.decision === "BLOCK" ? "badge-danger" : "badge-safe"}`}>
                                {log.decision}
                              </span>
                            </td>
                            <td className="reasons-cell">
                              {log.reasons.join(" • ")}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </section>
          )}

          {/* SIH COMPETITIVE EDGE / DIFFERENTIATORS */}
          {activeView === "differentiators" && (
            <section className="differentiators-layout">
              {/* Pitch Hero */}
              <div className="panel pitch-hero-panel">
                <div className="pitch-hero-badge">
                  <Zap size={15} />
                  <span>SMART INDIA HACKATHON 2026 — PROBLEM STATEMENT 26104</span>
                </div>
                <h2>Why VoiceShield Beats Existing Fraud & Deepfake Platforms</h2>
                <p>
                  Traditional deepfake tools and legacy biometrics only analyze audio in isolation. VoiceShield delivers an end-to-end cyber-defense platform purpose-built for India's banking and telecom infrastructure.
                </p>
              </div>

              {/* 5 Architectural Pillars */}
              <div className="pillar-grid">
                <div className="pillar-card">
                  <div className="pillar-icon cyan">
                    <Fingerprint size={22} />
                  </div>
                  <h4>1. Multi-Modal Contextual Risk Fusion</h4>
                  <p>
                    Generic detectors (e.g. ElevenLabs) only ask: <em>"Is this deepfaked?"</em> VoiceShield fuses <strong>AASIST Spectral Anomaly</strong> + <strong>ECAPA-TDNN 192-D Biometrics</strong> + <strong>Banking Context</strong> (transaction ₹ value, urgency pressure flags, and unverified beneficiaries) into a single deterministic score.
                  </p>
                  <span className="pillar-tag">4-Tier Risk Engine</span>
                </div>

                <div className="pillar-card">
                  <div className="pillar-icon emerald">
                    <Clock3 size={22} />
                  </div>
                  <h4>2. Sub-150ms In-Call WebSocket Defense</h4>
                  <p>
                    Existing platforms require <strong>3 to 10 seconds of recorded batch audio</strong> after the call is over. VoiceShield processes streaming 500ms sliding windows via low-latency WebSockets, intercepting voice scams <strong>before the OTP or money is transferred</strong>.
                  </p>
                  <span className="pillar-tag">Active Call Interception</span>
                </div>

                <div className="pillar-card">
                  <div className="pillar-icon purple">
                    <Lock size={22} />
                  </div>
                  <h4>3. India DPDP Act 2023 "Zero-Retention"</h4>
                  <p>
                    Foreign deepfake services upload raw audio to overseas cloud servers, violating Indian data sovereignty and RBI guidelines. VoiceShield processes audio in <strong>ephemeral volatile RAM with immediate cryptographic zeroing</strong>, generating SHA-256 session audit certificates.
                  </p>
                  <span className="pillar-tag">Zero Disk Persistence</span>
                </div>

                <div className="pillar-card">
                  <div className="pillar-icon amber">
                    <ShieldCheck size={22} />
                  </div>
                  <h4>4. Cryptographic Anti-Replay Engine</h4>
                  <p>
                    When attackers steal a real voice recording of a victim and replay it over the phone, standard biometrics are tricked. VoiceShield implements <strong>temporal session nonces and spectral phase drift detection</strong> to detect replayed bonafide audio.
                  </p>
                  <span className="pillar-tag">Anti-Replay Nonce</span>
                </div>
              </div>

              {/* Competitive Benchmark Matrix Table */}
              <div className="panel matrix-panel">
                <div className="panel-header">
                  <div>
                    <span className="panel-kicker">FEATURE-BY-FEATURE BENCHMARK</span>
                    <h3>VoiceShield vs. Existing Global Platforms</h3>
                  </div>
                  <span className="matrix-badge">SIH 2026 Evaluation Matrix</span>
                </div>

                <div className="matrix-table-wrapper">
                  <table className="matrix-table">
                    <thead>
                      <tr>
                        <th>CORE CAPABILITY</th>
                        <th>ELEVENLABS / SOTA CLOUD</th>
                        <th>MCAFEE SCAM DETECTOR</th>
                        <th>PINDROP / NUANCE BIOMETRICS</th>
                        <th className="highlight-col">VOICESHIELD (SIH 2026)</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr>
                        <td><strong>AI Deepfake Voice Detection</strong></td>
                        <td><span className="check-yes">✅ Yes (Acoustic only)</span></td>
                        <td><span className="check-yes">✅ Yes (Limited)</span></td>
                        <td><span className="check-partial">⚠️ Weak (No neural TTS focus)</span></td>
                        <td className="highlight-col"><span className="check-super">✅ Dual AASIST + Spectral Harmonics</span></td>
                      </tr>
                      <tr>
                        <td><strong>Speaker Identity Matching</strong></td>
                        <td><span className="check-no">❌ No</span></td>
                        <td><span className="check-no">❌ No</span></td>
                        <td><span className="check-yes">✅ Yes</span></td>
                        <td className="highlight-col"><span className="check-super">✅ 192-D ECAPA-TDNN Cosine Match</span></td>
                      </tr>
                      <tr>
                        <td><strong>Transaction & Urgency Context Fusion</strong></td>
                        <td><span className="check-no">❌ None (Audio-blind)</span></td>
                        <td><span className="check-no">❌ None</span></td>
                        <td><span className="check-no">❌ None</span></td>
                        <td className="highlight-col"><span className="check-super">✅ Full UPI / CBS Banking Integration</span></td>
                      </tr>
                      <tr>
                        <td><strong>In-Call Telephony Latency</strong></td>
                        <td><span className="check-no">❌ 3–8s Batch Delay</span></td>
                        <td><span className="check-no">❌ Post-Call Analysis</span></td>
                        <td><span className="check-partial">⚠️ 5–15s Passive Enrollment</span></td>
                        <td className="highlight-col"><span className="check-super">✅ &lt;150ms Sliding Window Streaming</span></td>
                      </tr>
                      <tr>
                        <td><strong>Anti-Replay Attack Protection</strong></td>
                        <td><span className="check-no">❌ Vulnerable to Replay</span></td>
                        <td><span className="check-no">❌ Vulnerable to Replay</span></td>
                        <td><span className="check-no">❌ Vulnerable to Replay</span></td>
                        <td className="highlight-col"><span className="check-super">✅ Cryptographic Temporal Nonces</span></td>
                      </tr>
                      <tr>
                        <td><strong>India DPDP Act 2023 Compliance</strong></td>
                        <td><span className="check-no">❌ US Cloud Storage</span></td>
                        <td><span className="check-no">❌ Persistent Cloud Logs</span></td>
                        <td><span className="check-partial">⚠️ Stores Permanent Biometrics</span></td>
                        <td className="highlight-col"><span className="check-super">✅ 100% Stateless RAM Zero-Retention</span></td>
                      </tr>
                      <tr>
                        <td><strong>Deterministic Decision Policies</strong></td>
                        <td><span className="check-no">❌ Just a % number</span></td>
                        <td><span className="check-no">❌ Informational alert</span></td>
                        <td><span className="check-partial">⚠️ Flag for agent review</span></td>
                        <td className="highlight-col"><span className="check-super">✅ Automated Action (BLOCK / STEP_UP / PASS)</span></td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </section>
          )}

          {/* ALERTS */}
          {activeView === "alerts" && (
            <section className="alerts-layout">
              <div className="panel">
                <div className="panel-header">
                  <div>
                    <span className="panel-kicker">
                      INCIDENT RESPONSE
                    </span>
                    <h3>Security Alerts</h3>
                  </div>

                  <span className="alert-count">
                    {alerts.filter(
                      (alert) =>
                        alert !==
                        "No unresolved security alerts"
                    ).length}{" "}
                    active
                  </span>
                </div>

                <div className="alert-list">
                  {alerts.map((alert, index) => (
                    <div
                      className="alert-row"
                      key={`${alert}-${index}`}
                    >
                      <div className="alert-icon">
                        {alert.includes("risk") ? (
                          <ShieldAlert size={17} />
                        ) : (
                          <ShieldCheck size={17} />
                        )}
                      </div>

                      <div>
                        <strong>{alert}</strong>
                        <span>
                          VoiceShield security monitoring
                          event
                        </span>
                      </div>

                      <span className="alert-time">
                        Just now
                      </span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="panel action-panel">
                <div className="panel-header">
                  <div>
                    <span className="panel-kicker">
                      RESPONSE PLAYBOOK
                    </span>
                    <h3>Risk Response</h3>
                  </div>
                </div>

                <div className="playbook-list">
                  <PlaybookItem
                    level="LOW"
                    text="Continue normal monitoring."
                  />
                  <PlaybookItem
                    level="MEDIUM"
                    text="Review the session and context."
                  />
                  <PlaybookItem
                    level="HIGH"
                    text="Initiate secondary verification."
                  />
                  <PlaybookItem
                    level="CRITICAL"
                    text="Hold high-risk transaction and escalate."
                  />
                </div>
              </div>
            </section>
          )}
        </main>
      </div>

      <footer className="global-footer">
        <div>
          VoiceShield Platform · Smart India Hackathon 2026
          · PSC26104
        </div>

        <div className="footer-right">
          <span>
            <span className="status-dot live" />
            Frontend Operational
          </span>
          <span>Security UX Prototype</span>
        </div>
      </footer>
    </div>
  );
}

function ServerIcon() {
  return (
    <span className="server-icon">
      <span />
      <span />
      <span />
    </span>
  );
}

function RiskBar({
  label,
  value,
  inverse = false,
}: {
  label: string;
  value: number;
  inverse?: boolean;
}) {
  return (
    <div className="risk-bar-block">
      <div className="risk-bar-label">
        <span>{label}</span>
        <strong>{value}%</strong>
      </div>

      <div className="progress-track">
        <div
          className={`progress-fill ${
            inverse ? "inverse" : ""
          }`}
          style={{
            width: `${scoreWidth(value)}%`,
          }}
        />
      </div>
    </div>
  );
}

function DetailRow({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="detail-row">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function AnalysisStat({
  label,
  value,
  progress,
  icon,
  text = false,
}: {
  label: string;
  value: string;
  progress?: number;
  icon: React.ReactNode;
  text?: boolean;
}) {
  return (
    <div className="analysis-stat">
      <div className="analysis-stat-top">
        <span>{label}</span>
        {icon}
      </div>

      <strong>{value}</strong>

      {!text && typeof progress === "number" ? (
        <div className="progress-track compact">
          <div
            className="progress-fill"
            style={{
              width: `${scoreWidth(progress)}%`,
            }}
          />
        </div>
      ) : (
        <small>Context evaluation</small>
      )}
    </div>
  );
}

function EmptyState({
  icon,
  title,
  text,
}: {
  icon: React.ReactNode;
  title: string;
  text: string;
}) {
  return (
    <div className="empty-state">
      <div className="empty-icon">{icon}</div>
      <strong>{title}</strong>
      <span>{text}</span>
    </div>
  );
}

function ProcessStep({
  number,
  label,
}: {
  number: string;
  label: string;
}) {
  return (
    <div className="process-step">
      <span>{number}</span>
      <strong>{label}</strong>
    </div>
  );
}

function ProcessLine() {
  return <div className="process-line" />;
}

function PlaybookItem({
  level,
  text,
}: {
  level: string;
  text: string;
}) {
  return (
    <div className="playbook-item">
      <span className={`risk-badge ${riskBadgeClass(level)}`}>
        {level}
      </span>

      <span>{text}</span>
    </div>
  );
}
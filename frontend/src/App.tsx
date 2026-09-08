import React, { useMemo, useState } from "react";
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

type View = "overview" | "analysis" | "live" | "alerts";

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

  const [verificationState, setVerificationState] =
    useState<VerificationState>("NOT_REQUIRED");

  const [alerts, setAlerts] = useState<string[]>([
    "No unresolved security alerts",
  ]);

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
                    REAL-TIME MONITOR
                  </span>
                  <h3>Live Voice Integrity Stream</h3>
                  <p>
                    Frontend monitoring interface ready for
                    WebSocket telemetry integration.
                  </p>
                </div>

                <div className="live-controls">
                  <span
                    className={`connection-status ${
                      liveConnected ? "connected" : ""
                    }`}
                  >
                    <span />
                    {liveConnected
                      ? "CONNECTED"
                      : "STANDBY"}
                  </span>

                  <button
                    className="primary-button"
                    onClick={() => {
                      setLiveConnected(
                        (current) => !current
                      );

                      if (!liveConnected) {
                        setLiveRisk(28);
                        setLiveTier("MEDIUM");
                      } else {
                        setLiveRisk(12);
                        setLiveTier("LOW");
                      }
                    }}
                  >
                    {liveConnected ? (
                      <>
                        <MicOff size={16} />
                        Stop Monitoring
                      </>
                    ) : (
                      <>
                        <Mic size={16} />
                        Start Monitoring
                      </>
                    )}
                  </button>
                </div>
              </div>

              <div className="metric-grid live-metrics">
                <div className="metric-card large">
                  <div className="metric-card-top">
                    <span>LIVE RISK SCORE</span>
                    <Activity size={17} />
                  </div>
                  <strong>{liveConnected ? liveRisk : "--"}</strong>
                  <small>
                    Rolling real-time assessment
                  </small>
                </div>

                <div className="metric-card large">
                  <div className="metric-card-top">
                    <span>DECISION TIER</span>
                    <ShieldAlert size={17} />
                  </div>
                  <strong>
                    {liveConnected ? liveTier : "IDLE"}
                  </strong>
                  <small>
                    Auto-escalation interface
                  </small>
                </div>

                <div className="metric-card large">
                  <div className="metric-card-top">
                    <span>STREAM STATUS</span>
                    <Wifi size={17} />
                  </div>
                  <strong>
                    {liveConnected
                      ? "ONLINE"
                      : "STANDBY"}
                  </strong>
                  <small>
                    WebSocket-ready frontend
                  </small>
                </div>
              </div>

              <div className="panel waveform-panel">
                <div className="panel-header">
                  <div>
                    <span className="panel-kicker">
                      TELEMETRY
                    </span>
                    <h3>Audio Activity Monitor</h3>
                  </div>

                  <span className="live-indicator">
                    <span />
                    LIVE
                  </span>
                </div>

                <div className="waveform">
                  {Array.from({
                    length: 44,
                  }).map((_, index) => {
                    const height =
                      liveConnected
                        ? 15 +
                          ((index * 17) % 58)
                        : 10 + ((index * 7) % 20);

                    return (
                      <span
                        key={index}
                        style={{
                          height: `${height}px`,
                        }}
                      />
                    );
                  })}
                </div>

                <div className="waveform-footer">
                  <span>00:00</span>
                  <span>
                    Stream telemetry visualization
                  </span>
                  <span>LIVE</span>
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
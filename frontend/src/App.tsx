import React, { useState, useRef } from 'react';
import {
  ShieldAlert,
  ShieldCheck,
  Activity,
  Mic,
  MicOff,
  Upload,
  Radio,
  Lock,
  FileAudio,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  TrendingUp,
  Server,
  Fingerprint,
  Waves
} from 'lucide-react';
import { AudioAnalysisResponse } from './types/index.ts';

const BACKEND_URL = 'http://localhost:8000/api/v1';

export default function App() {
  const [activeTab, setActiveTab] = useState<'upload' | 'live' | 'enroll'>('upload');
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [result, setResult] = useState<AudioAnalysisResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Context form inputs
  const [claimedSpeaker, setClaimedSpeaker] = useState<string>('VIP-CEO-01');
  const [transactionAmount, setTransactionAmount] = useState<string>('500000');
  const [urgencyFlag, setUrgencyFlag] = useState<boolean>(true);
  const [isNewBeneficiary, setIsNewBeneficiary] = useState<boolean>(true);

  // Live stream state
  const [isStreaming, setIsStreaming] = useState<boolean>(false);
  const [liveScore, setLiveScore] = useState<number>(12);
  const [liveTier, setLiveTier] = useState<string>('LOW');
  const wsRef = useRef<WebSocket | null>(null);

  const handleFileUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file) {
      setError('Please select an audio file (WAV, MP3, FLAC) to analyze.');
      return;
    }

    setLoading(true);
    setError(null);

    const formData = new FormData();
    formData.append('file', file);
    if (claimedSpeaker) formData.append('claimed_speaker_id', claimedSpeaker);
    if (transactionAmount) formData.append('transaction_amount', transactionAmount);
    formData.append('urgency_flag', urgencyFlag ? 'true' : 'false');
    formData.append('is_new_beneficiary', isNewBeneficiary ? 'true' : 'false');

    try {
      const response = await fetch(`${BACKEND_URL}/audio/analyze`, {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        throw new Error(`Server returned error: ${response.statusText}`);
      }

      const data: AudioAnalysisResponse = await response.json();
      setResult(data);
    } catch (err: any) {
      setError(err.message || 'Failed to connect to VoiceShield Backend.');
    } finally {
      setLoading(false);
    }
  };

  const getTierColor = (tier: string) => {
    switch (tier) {
      case 'CRITICAL':
        return 'text-rose-500 bg-rose-950/40 border-rose-500/50';
      case 'HIGH':
        return 'text-amber-400 bg-amber-950/40 border-amber-500/50';
      case 'MEDIUM':
        return 'text-yellow-400 bg-yellow-950/40 border-yellow-500/50';
      default:
        return 'text-emerald-400 bg-emerald-950/40 border-emerald-500/50';
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-indigo-500 selection:text-white">
      {/* Header */}
      <header className="border-b border-slate-800 bg-slate-900/60 backdrop-blur-md sticky top-0 z-50 px-6 py-3.5 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 via-blue-600 to-cyan-400 flex items-center justify-center shadow-lg shadow-indigo-500/20">
            <Radio className="w-5 h-5 text-white animate-pulse" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h1 className="text-xl font-bold tracking-tight bg-gradient-to-r from-white via-slate-100 to-slate-400 bg-clip-text text-transparent">
                VoiceShield
              </h1>
              <span className="text-xs px-2 py-0.5 rounded-full font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                SIH 2026
              </span>
            </div>
            <p className="text-xs text-slate-400">AI Real-Time Voice Integrity & Impersonation Prevention</p>
          </div>
        </div>

        <div className="flex items-center space-x-4">
          <div className="flex items-center space-x-2 bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-300">
            <Lock className="w-3.5 h-3.5 text-emerald-400" />
            <span>DPDP Zero-Retention Active</span>
          </div>
          <div className="flex items-center space-x-2 bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-300">
            <Server className="w-3.5 h-3.5 text-indigo-400" />
            <span>FastAPI v1.0.0</span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-6 space-y-6">
        {/* Navigation Tabs */}
        <div className="flex space-x-2 border-b border-slate-800 pb-2">
          <button
            onClick={() => setActiveTab('upload')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-sm font-medium transition ${
              activeTab === 'upload'
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-900'
            }`}
          >
            <Upload className="w-4 h-4" />
            <span>Forensic Audio Analysis</span>
          </button>
          <button
            onClick={() => setActiveTab('live')}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-sm font-medium transition ${
              activeTab === 'live'
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                : 'text-slate-400 hover:text-white hover:bg-slate-900'
            }`}
          >
            <Radio className="w-4 h-4" />
            <span>Live Call Intercept Stream</span>
          </button>
        </div>

        {/* Forensic Upload Analysis Tab */}
        {activeTab === 'upload' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Left Column: Form Controls */}
            <div className="lg:col-span-4 space-y-6">
              <form onSubmit={handleFileUpload} className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-4">
                <h2 className="text-base font-semibold text-slate-100 flex items-center space-x-2">
                  <FileAudio className="w-4 h-4 text-indigo-400" />
                  <span>Audio & Transaction Context</span>
                </h2>

                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Audio File (WAV, MP3, FLAC)</label>
                  <input
                    type="file"
                    accept="audio/*"
                    onChange={(e) => setFile(e.target.files ? e.target.files[0] : null)}
                    className="w-full text-xs text-slate-300 file:mr-3 file:py-2 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-indigo-600 file:text-white hover:file:bg-indigo-500 cursor-pointer bg-slate-950 border border-slate-800 rounded-xl p-2"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Claimed Speaker ID (Enrolled VIP)</label>
                  <input
                    type="text"
                    value={claimedSpeaker}
                    onChange={(e) => setClaimedSpeaker(e.target.value)}
                    placeholder="e.g. VIP-CEO-01"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-400 mb-1">Requested Transaction Value (INR)</label>
                  <input
                    type="number"
                    value={transactionAmount}
                    onChange={(e) => setTransactionAmount(e.target.value)}
                    placeholder="500000"
                    className="w-full bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
                  />
                </div>

                <div className="space-y-2 pt-2 border-t border-slate-800/80">
                  <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={urgencyFlag}
                      onChange={(e) => setUrgencyFlag(e.target.checked)}
                      className="rounded bg-slate-950 border-slate-700 text-indigo-600 focus:ring-0"
                    />
                    <span>Urgent Social Engineering Indicator</span>
                  </label>
                  <label className="flex items-center space-x-2 text-xs text-slate-300 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={isNewBeneficiary}
                      onChange={(e) => setIsNewBeneficiary(e.target.checked)}
                      className="rounded bg-slate-950 border-slate-700 text-indigo-600 focus:ring-0"
                    />
                    <span>Unverified Beneficiary Account</span>
                  </label>
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-2.5 rounded-xl bg-gradient-to-r from-indigo-600 to-blue-600 hover:from-indigo-500 hover:to-blue-500 font-semibold text-xs shadow-lg shadow-indigo-600/20 text-white transition disabled:opacity-50 flex items-center justify-center space-x-2"
                >
                  {loading ? (
                    <>
                      <Activity className="w-4 h-4 animate-spin" />
                      <span>Running Neural Multi-Layer Inspection...</span>
                    </>
                  ) : (
                    <>
                      <ShieldCheck className="w-4 h-4" />
                      <span>Inspect Voice Authenticity</span>
                    </>
                  )}
                </button>
              </form>

              {error && (
                <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-500/30 text-rose-300 text-xs flex items-start space-x-2">
                  <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
                  <span>{error}</span>
                </div>
              )}
            </div>

            {/* Right Column: Assessment HUD */}
            <div className="lg:col-span-8 space-y-6">
              {result ? (
                <>
                  {/* Top Alert Banner */}
                  <div className={`p-5 rounded-2xl border ${getTierColor(result.risk_tier)} flex items-center justify-between`}>
                    <div className="flex items-center space-x-4">
                      <div className="w-14 h-14 rounded-2xl bg-slate-950/60 flex items-center justify-center border border-white/10 shrink-0">
                        {result.risk_tier === 'CRITICAL' || result.risk_tier === 'HIGH' ? (
                          <ShieldAlert className="w-7 h-7 text-rose-500 animate-bounce" />
                        ) : (
                          <ShieldCheck className="w-7 h-7 text-emerald-400" />
                        )}
                      </div>
                      <div>
                        <div className="flex items-center space-x-2">
                          <span className="text-xl font-bold tracking-tight">Risk Tier: {result.risk_tier}</span>
                          <span className="text-xs px-2.5 py-0.5 rounded-full font-mono font-semibold bg-slate-950/80 border border-white/10">
                            Score: {result.overall_risk_score}/100
                          </span>
                        </div>
                        <p className="text-xs mt-1 text-slate-300 font-medium">Action: {result.action_required}</p>
                      </div>
                    </div>
                  </div>

                  {/* Recommendations */}
                  <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-3">
                    <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center space-x-2">
                      <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
                      <span>Actionable Countermeasures</span>
                    </h3>
                    <ul className="space-y-2">
                      {result.recommendations.map((rec, idx) => (
                        <li key={idx} className="text-xs text-slate-300 flex items-start space-x-2 bg-slate-950/50 p-2.5 rounded-lg border border-slate-800/60">
                          <span className="text-indigo-400 font-mono font-bold">{idx + 1}.</span>
                          <span>{rec}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  {/* Multi-Layer Metrics Grid */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {/* Neural Deepfake Layer */}
                    <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 space-y-3">
                      <h4 className="text-xs font-semibold text-slate-300 flex items-center justify-between">
                        <span className="flex items-center space-x-1.5">
                          <Activity className="w-3.5 h-3.5 text-indigo-400" />
                          <span>AI Deepfake Classifier</span>
                        </span>
                        <span className={`font-mono text-xs font-bold ${result.classification === 'SPOOF' ? 'text-rose-400' : 'text-emerald-400'}`}>
                          {result.classification}
                        </span>
                      </h4>
                      <div className="space-y-1.5 text-xs text-slate-400">
                        <div className="flex justify-between">
                          <span>Synthetic Probability:</span>
                          <span className="font-mono text-slate-200">{(result.synthetic_probability * 100).toFixed(1)}%</span>
                        </div>
                        <div className="flex justify-between">
                          <span>Bonafide (Human) Prob:</span>
                          <span className="font-mono text-slate-200">{(result.bonafide_probability * 100).toFixed(1)}%</span>
                        </div>
                      </div>
                    </div>

                    {/* Prosodic Rhythm Layer */}
                    <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 space-y-3">
                      <h4 className="text-xs font-semibold text-slate-300 flex items-center space-x-1.5">
                        <Waves className="w-3.5 h-3.5 text-cyan-400" />
                        <span>Prosodic & Rhythm Dynamics</span>
                      </h4>
                      <div className="space-y-1.5 text-xs text-slate-400">
                        <div className="flex justify-between">
                          <span>Pitch F0 Mean:</span>
                          <span className="font-mono text-slate-200">{result.prosodic_dynamics.mean_pitch_f0_hz} Hz</span>
                        </div>
                        <div className="flex justify-between">
                          <span>Jitter Perturbation:</span>
                          <span className="font-mono text-slate-200">{result.prosodic_dynamics.jitter_percent}%</span>
                        </div>
                        <div className="flex justify-between">
                          <span>Shimmer Perturbation:</span>
                          <span className="font-mono text-slate-200">{result.prosodic_dynamics.shimmer_percent}%</span>
                        </div>
                      </div>
                    </div>

                    {/* Spectral Biometrics */}
                    <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 space-y-3">
                      <h4 className="text-xs font-semibold text-slate-300 flex items-center space-x-1.5">
                        <TrendingUp className="w-3.5 h-3.5 text-purple-400" />
                        <span>Spectral Artifacts</span>
                      </h4>
                      <div className="space-y-1.5 text-xs text-slate-400">
                        <div className="flex justify-between">
                          <span>Spectral Centroid:</span>
                          <span className="font-mono text-slate-200">{result.spectral_biometrics.spectral_centroid_hz} Hz</span>
                        </div>
                        <div className="flex justify-between">
                          <span>Spectral Flatness:</span>
                          <span className="font-mono text-slate-200">{result.spectral_biometrics.spectral_flatness}</span>
                        </div>
                        <div className="flex justify-between">
                          <span>High-Freq Ratio (&gt;4kHz):</span>
                          <span className="font-mono text-slate-200">{result.spectral_biometrics.high_freq_energy_ratio}</span>
                        </div>
                      </div>
                    </div>

                    {/* Speaker Verification */}
                    <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 space-y-3">
                      <h4 className="text-xs font-semibold text-slate-300 flex items-center space-x-1.5">
                        <Fingerprint className="w-3.5 h-3.5 text-emerald-400" />
                        <span>Speaker Identity Verification</span>
                      </h4>
                      <div className="space-y-1.5 text-xs text-slate-400">
                        <div className="flex justify-between">
                          <span>Enrolled Reference:</span>
                          <span className="font-mono text-slate-200">{result.speaker_verification?.speaker_id || 'None'}</span>
                        </div>
                        <div className="flex justify-between">
                          <span>Identity Match:</span>
                          <span className="font-mono text-slate-200">
                            {result.speaker_verification?.verified ? 'VERIFIED MATCH' : 'MISMATCH / UNENROLLED'}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </>
              ) : (
                <div className="h-80 rounded-2xl border border-dashed border-slate-800 bg-slate-900/30 flex flex-col items-center justify-center text-center p-6 space-y-3">
                  <div className="w-12 h-12 rounded-full bg-slate-800/80 flex items-center justify-center text-slate-400">
                    <Activity className="w-6 h-6" />
                  </div>
                  <h3 className="text-sm font-semibold text-slate-300">Awaiting Audio Submission</h3>
                  <p className="text-xs text-slate-500 max-w-sm">
                    Select an audio sample on the left panel to execute multi-layer deepfake, prosody, and contextual threat analysis.
                  </p>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Live Stream Intercept Tab */}
        {activeTab === 'live' && (
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-bold text-white flex items-center space-x-2">
                  <Radio className="w-5 h-5 text-indigo-400 animate-pulse" />
                  <span>Real-Time Telephony Call Intercept Stream</span>
                </h2>
                <p className="text-xs text-slate-400 mt-1">
                  Continuous rolling-window audio inspection over WebSocket (<span className="font-mono text-indigo-300">ws://localhost:8000/api/v1/ws/live-stream</span>)
                </p>
              </div>
              <button
                onClick={() => setIsStreaming(!isStreaming)}
                className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center space-x-2 transition ${
                  isStreaming
                    ? 'bg-rose-600 hover:bg-rose-500 text-white shadow-lg shadow-rose-600/30'
                    : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-600/30'
                }`}
              >
                {isStreaming ? (
                  <>
                    <MicOff className="w-4 h-4" />
                    <span>Stop Intercept Stream</span>
                  </>
                ) : (
                  <>
                    <Mic className="w-4 h-4" />
                    <span>Start Live Audio Stream</span>
                  </>
                )}
              </button>
            </div>

            {/* Live Visualizer HUD */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="p-5 rounded-xl bg-slate-950 border border-slate-800 flex flex-col items-center justify-center text-center space-y-2">
                <span className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Live Smoothed Risk</span>
                <span className="text-4xl font-bold font-mono text-emerald-400">{isStreaming ? liveScore : '--'}</span>
                <span className="text-xs text-slate-500">Exponential Moving Average (EMA)</span>
              </div>
              <div className="p-5 rounded-xl bg-slate-950 border border-slate-800 flex flex-col items-center justify-center text-center space-y-2">
                <span className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Current Decision Tier</span>
                <span className="text-2xl font-bold font-mono text-indigo-300">{isStreaming ? liveTier : 'IDLE'}</span>
                <span className="text-xs text-slate-500">Auto-Escalation Enabled</span>
              </div>
              <div className="p-5 rounded-xl bg-slate-950 border border-slate-800 flex flex-col items-center justify-center text-center space-y-2">
                <span className="text-xs text-slate-400 font-semibold uppercase tracking-wider">Stream Protocol</span>
                <span className="text-xl font-bold font-mono text-slate-200">PCM 16kHz / 30ms</span>
                <span className="text-xs text-emerald-400">Low Latency (&lt;120ms)</span>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 bg-slate-950 px-6 py-4 text-center text-xs text-slate-500">
        VoiceShield Platform • Smart India Hackathon 2026 (Problem Statement 26104) • Built for Team Collaboration
      </footer>
    </div>
  );
}

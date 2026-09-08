import React, { useState, useRef, useEffect } from 'react';
import { 
  Shield, 
  ShieldAlert, 
  ShieldCheck, 
  UploadCloud, 
  FileAudio, 
  Activity, 
  Cpu, 
  Sparkles, 
  CheckCircle2, 
  AlertTriangle, 
  Clock, 
  BarChart3, 
  Layers, 
  Volume2, 
  Terminal, 
  RefreshCw 
} from 'lucide-react';
import { AudioAnalysisResponse } from './types';

export default function App() {
  const [file, setFile] = useState<File | null>(null);
  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState<boolean>(false);
  const [analysisResult, setAnalysisResult] = useState<AudioAnalysisResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState<boolean>(false);
  const [showJsonPayload, setShowJsonPayload] = useState<boolean>(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  // Clean up object URLs to prevent memory leaks
  useEffect(() => {
    return () => {
      if (audioUrl) URL.revokeObjectURL(audioUrl);
    };
  }, [audioUrl]);

  const handleFileSelection = (selectedFile: File) => {
    setError(null);
    setAnalysisResult(null);

    const validExtensions = ['wav', 'mp3', 'm4a', 'flac', 'ogg'];
    const ext = selectedFile.name.split('.').pop()?.toLowerCase() || '';

    if (!validExtensions.includes(ext)) {
      setError(`Unsupported format ".${ext}". Please upload WAV, MP3, M4A, FLAC, or OGG.`);
      return;
    }

    if (selectedFile.size > 15 * 1024 * 1024) {
      setError(`File size exceeds 15 MB limit (${(selectedFile.size / (1024 * 1024)).toFixed(1)} MB).`);
      return;
    }

    setFile(selectedFile);
    if (audioUrl) URL.revokeObjectURL(audioUrl);
    setAudioUrl(URL.createObjectURL(selectedFile));
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileSelection(e.dataTransfer.files[0]);
    }
  };

  const createSyntheticTestAudio = (isSpoofCandidate: boolean) => {
    // Generates client-side synthetic audio test samples to demonstrate bonafide vs spoof characteristics
    const sampleRate = 16000;
    const duration = 2.0;
    const numSamples = sampleRate * duration;
    const buffer = new Float32Array(numSamples);

    for (let i = 0; i < numSamples; i++) {
      const t = i / sampleRate;
      if (isSpoofCandidate) {
        // High frequency vocoder artifacts + flat spectral harmonics + unmodulated phase dispersion
        buffer[i] = (
          0.20 * Math.sin(2 * Math.PI * 220 * t) +
          0.20 * Math.sin(2 * Math.PI * 440 * t) +
          0.25 * Math.sin(2 * Math.PI * 880 * t) +
          0.20 * Math.sin(2 * Math.PI * 1760 * t) +
          0.15 * Math.sin(2 * Math.PI * 3520 * t) +
          0.10 * Math.sin(2 * Math.PI * 5200 * t) +
          (Math.random() - 0.5) * 0.12
        );
      } else {
        // Human speech-like formant harmonics with pitch modulation & natural resonant decay
        const f0 = 130 + 15 * Math.sin(2 * Math.PI * 3 * t);
        buffer[i] = (
          0.55 * Math.sin(2 * Math.PI * f0 * t) +
          0.25 * Math.sin(2 * Math.PI * 2 * f0 * t) +
          0.10 * Math.sin(2 * Math.PI * 3 * f0 * t)
        );
      }
    }

    // Convert Float32Array to 16-bit PCM WAV Blob
    const wavBuffer = new ArrayBuffer(44 + numSamples * 2);
    const view = new DataView(wavBuffer);

    // RIFF chunk descriptor
    writeString(view, 0, 'RIFF');
    view.setUint32(4, 36 + numSamples * 2, true);
    writeString(view, 8, 'WAVE');

    // fmt sub-chunk
    writeString(view, 12, 'fmt ');
    view.setUint32(16, 16, true);
    view.setUint16(20, 1, true); // PCM format
    view.setUint16(22, 1, true); // Mono channel
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, sampleRate * 2, true); // Byte rate
    view.setUint16(32, 2, true); // Block align
    view.setUint16(34, 16, true); // Bits per sample

    // data sub-chunk
    writeString(view, 36, 'data');
    view.setUint32(40, numSamples * 2, true);

    let offset = 44;
    for (let i = 0; i < numSamples; i++) {
      const s = Math.max(-1, Math.min(1, buffer[i]));
      view.setInt16(offset, s < 0 ? s * 0x8000 : s * 0x7FFF, true);
      offset += 2;
    }

    const testBlob = new Blob([view], { type: 'audio/wav' });
    const testFile = new File(
      [testBlob],
      isSpoofCandidate ? 'synthetic_voice_sample.wav' : 'genuine_human_sample.wav',
      { type: 'audio/wav' }
    );

    handleFileSelection(testFile);
  };

  const writeString = (view: DataView, offset: number, str: string) => {
    for (let i = 0; i < str.length; i++) {
      view.setUint8(offset + i, str.charCodeAt(i));
    }
  };

  const analyzeAudio = async () => {
    if (!file) return;

    setIsAnalyzing(true);
    setError(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch('/api/v1/audio/analyze', {
        method: 'POST',
        body: formData,
      });

      if (!response.ok) {
        const errJson = await response.json().catch(() => ({ detail: 'Analysis failed' }));
        throw new Error(errJson.detail || `Server responded with HTTP ${response.status}`);
      }

      const data: AudioAnalysisResponse = await response.json();
      setAnalysisResult(data);
    } catch (err: any) {
      setError(err.message || 'An unexpected error occurred during audio processing.');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const getRiskColor = (level?: string) => {
    switch (level) {
      case 'CRITICAL':
        return 'text-rose-400 bg-rose-950/60 border-rose-800/80 shadow-rose-900/40';
      case 'HIGH':
        return 'text-amber-400 bg-amber-950/60 border-amber-800/80 shadow-amber-900/40';
      case 'MEDIUM':
        return 'text-yellow-400 bg-yellow-950/60 border-yellow-800/80 shadow-yellow-900/40';
      case 'LOW':
      default:
        return 'text-emerald-400 bg-emerald-950/60 border-emerald-800/80 shadow-emerald-900/40';
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between selection:bg-cyan-500 selection:text-white">
      {/* Top Cyber Defense Navigation Bar */}
      <header className="border-b border-slate-800/80 bg-slate-900/50 backdrop-blur-xl sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-gradient-to-br from-cyan-500 to-blue-600 shadow-lg shadow-cyan-500/20 text-white">
              <Shield className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-white via-slate-200 to-cyan-400 bg-clip-text text-transparent">
                  VoiceShield
                </span>
                <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/30">
                  SIH 2026 #26104
                </span>
              </div>
              <p className="text-[11px] text-slate-400 font-mono">
                Real-Time AI Voice Integrity & Anti-Spoofing Platform
              </p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden md:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 text-xs text-slate-300">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
              <span className="font-mono">AASIST Neural Pipeline: Active</span>
            </div>
            <div className="px-2.5 py-1 rounded-md bg-slate-800 text-slate-300 text-xs font-mono font-medium border border-slate-700">
              PHASE 1 : BASELINE
            </div>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full flex-grow">
        {/* Banner Section */}
        <div className="mb-8 p-6 rounded-2xl bg-gradient-to-r from-slate-900 via-slate-900 to-cyan-950/40 border border-slate-800 shadow-xl relative overflow-hidden">
          <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-cyan-500/5 rounded-full blur-3xl pointer-events-none"></div>
          <div className="relative z-10">
            <div className="flex items-center gap-2 text-cyan-400 text-xs font-semibold uppercase tracking-wider mb-2">
              <Cpu className="w-4 h-4" /> AI Audio Anti-Spoofing Inference Engine
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Voice Cloning & Synthetic Speech Detection
            </h1>
            <p className="mt-2 text-sm text-slate-400 max-w-3xl leading-relaxed">
              Analyze incoming voice recordings to classify genuine human speech (<code className="text-emerald-400">bonafide</code>) vs. AI-synthesized, cloned, or neural vocoder spoofing attacks (<code className="text-rose-400">spoof</code>) using spectro-temporal graph attention representations.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Left Column: Upload & Controls */}
          <div className="lg:col-span-5 space-y-6">
            {/* Upload Box */}
            <div className="p-6 rounded-2xl bg-slate-900/80 border border-slate-800 shadow-lg">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-base font-semibold text-white flex items-center gap-2">
                  <UploadCloud className="w-5 h-5 text-cyan-400" /> Upload Audio Recording
                </h2>
                <span className="text-xs text-slate-400 font-mono">WAV, MP3, M4A, FLAC</span>
              </div>

              <div
                onDragEnter={handleDrag}
                onDragLeave={handleDrag}
                onDragOver={handleDrag}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
                className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all duration-200 flex flex-col items-center justify-center gap-3 ${
                  dragActive
                    ? 'border-cyan-400 bg-cyan-950/20'
                    : 'border-slate-700/80 bg-slate-950/50 hover:border-slate-600 hover:bg-slate-900/50'
                }`}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="audio/*,.wav,.mp3,.m4a,.flac,.ogg"
                  className="hidden"
                  onChange={(e) => {
                    if (e.target.files && e.target.files[0]) {
                      handleFileSelection(e.target.files[0]);
                    }
                  }}
                />

                <div className="w-12 h-12 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
                  <FileAudio className="w-6 h-6" />
                </div>
                <div>
                  <p className="text-sm font-medium text-slate-200">
                    Click to browse or drag & drop audio file
                  </p>
                  <p className="text-xs text-slate-500 mt-1">
                    Maximum audio size: 15 MB (Processed in-memory)
                  </p>
                </div>
              </div>

              {/* Sample Presets for Hackathon Testing */}
              <div className="mt-4 pt-4 border-t border-slate-800">
                <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-cyan-400" /> Quick Test Samples (SIH Judges Demo)
                </p>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => createSyntheticTestAudio(false)}
                    className="px-3 py-2 text-xs rounded-lg bg-slate-800/80 hover:bg-slate-800 text-emerald-400 border border-emerald-900/50 hover:border-emerald-700/70 font-medium transition flex items-center justify-center gap-1.5"
                  >
                    <CheckCircle2 className="w-3.5 h-3.5" /> Sample Human
                  </button>
                  <button
                    type="button"
                    onClick={() => createSyntheticTestAudio(true)}
                    className="px-3 py-2 text-xs rounded-lg bg-slate-800/80 hover:bg-slate-800 text-rose-400 border border-rose-900/50 hover:border-rose-700/70 font-medium transition flex items-center justify-center gap-1.5"
                  >
                    <AlertTriangle className="w-3.5 h-3.5" /> Sample AI Clone
                  </button>
                </div>
              </div>

              {/* Selected File Details & Audio Preview */}
              {file && (
                <div className="mt-4 p-4 rounded-xl bg-slate-950 border border-slate-800/90 space-y-3">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-mono text-slate-300 truncate max-w-[200px]" title={file.name}>
                      {file.name}
                    </span>
                    <span className="font-mono text-slate-500">
                      {(file.size / 1024).toFixed(1)} KB
                    </span>
                  </div>

                  {audioUrl && (
                    <div className="w-full">
                      <audio src={audioUrl} controls className="w-full h-8 rounded" />
                    </div>
                  )}

                  <button
                    onClick={analyzeAudio}
                    disabled={isAnalyzing}
                    className="w-full mt-2 py-2.5 px-4 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white font-semibold text-sm shadow-lg shadow-cyan-500/20 transition duration-150 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
                  >
                    {isAnalyzing ? (
                      <>
                        <RefreshCw className="w-4 h-4 animate-spin" />
                        Analyzing Voice Spectrum & Graph Embeddings...
                      </>
                    ) : (
                      <>
                        <Activity className="w-4 h-4" />
                        Run Voice Authenticity Analysis
                      </>
                    )}
                  </button>
                </div>
              )}

              {error && (
                <div className="mt-4 p-3 rounded-lg bg-rose-950/50 border border-rose-800/60 text-rose-300 text-xs flex items-start gap-2">
                  <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5 text-rose-400" />
                  <span>{error}</span>
                </div>
              )}
            </div>

            {/* Architecture Details Card */}
            <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800/80 text-xs text-slate-400 space-y-2.5">
              <h3 className="font-semibold text-slate-200 flex items-center gap-2">
                <Layers className="w-4 h-4 text-cyan-400" /> Phase 1 Pipeline Architecture
              </h3>
              <p>
                1. <strong>Audio Ingestion</strong>: 16kHz mono normalization & VAD silence trimming.
              </p>
              <p>
                2. <strong>AASIST Model</strong>: SincNet raw waveform filterbank & Spectro-Temporal Graph Attention.
              </p>
              <p>
                3. <strong>Probabilistic Output</strong>: Classifies Bonafide Human vs Synthetic/Spoofed Speech.
              </p>
            </div>
          </div>

          {/* Right Column: Analysis Results Display */}
          <div className="lg:col-span-7">
            {analysisResult ? (
              <div className="space-y-6">
                {/* Result Card */}
                <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 shadow-xl space-y-6">
                  <div className="flex items-center justify-between pb-4 border-b border-slate-800">
                    <div>
                      <span className="text-xs font-mono uppercase tracking-wider text-slate-400">
                        Voice Authenticity Assessment
                      </span>
                      <h2 className="text-xl font-bold text-white mt-0.5">
                        Inference Analysis Report
                      </h2>
                    </div>

                    <div className={`px-4 py-2 rounded-xl border text-xs font-bold uppercase tracking-wider flex items-center gap-2 ${getRiskColor(analysisResult.risk_level)}`}>
                      {analysisResult.prediction === 'bonafide' ? (
                        <>
                          <ShieldCheck className="w-4 h-4" />
                          <span>GENUINE HUMAN SPEECH</span>
                        </>
                      ) : (
                        <>
                          <ShieldAlert className="w-4 h-4" />
                          <span>SYNTHETIC / CLONED SPOOF</span>
                        </>
                      )}
                    </div>
                  </div>

                  {/* Probabilities Comparison Bar */}
                  <div className="space-y-4">
                    <div>
                      <div className="flex justify-between text-xs font-medium mb-1.5">
                        <span className="text-slate-300 flex items-center gap-1.5">
                          <AlertTriangle className="w-3.5 h-3.5 text-rose-400" /> AI Synthetic Voice Probability
                        </span>
                        <span className="font-mono font-bold text-rose-400">
                          {(analysisResult.synthetic_probability * 100).toFixed(1)}%
                        </span>
                      </div>
                      <div className="w-full h-3 rounded-full bg-slate-950 border border-slate-800 overflow-hidden">
                        <div
                          className="h-full bg-gradient-to-r from-amber-500 to-rose-500 rounded-full transition-all duration-700"
                          style={{ width: `${Math.max(2, analysisResult.synthetic_probability * 100)}%` }}
                        ></div>
                      </div>
                    </div>

                    <div>
                      <div className="flex justify-between text-xs font-medium mb-1.5">
                        <span className="text-slate-300 flex items-center gap-1.5">
                          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" /> Human Authenticity Probability
                        </span>
                        <span className="font-mono font-bold text-emerald-400">
                          {(analysisResult.human_probability * 100).toFixed(1)}%
                        </span>
                      </div>
                      <div className="w-full h-3 rounded-full bg-slate-950 border border-slate-800 overflow-hidden">
                        <div
                          className="h-full bg-gradient-to-r from-teal-500 to-emerald-400 rounded-full transition-all duration-700"
                          style={{ width: `${Math.max(2, analysisResult.human_probability * 100)}%` }}
                        ></div>
                      </div>
                    </div>
                  </div>

                  {/* Metadata & Biometrics Grid */}
                  <div className="pt-4 border-t border-slate-800">
                    <h3 className="text-xs font-semibold text-slate-300 uppercase tracking-wider mb-3 flex items-center gap-2">
                      <BarChart3 className="w-4 h-4 text-cyan-400" /> Acoustic Biometrics & Signal Properties
                    </h3>

                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                      <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800/80">
                        <p className="text-[11px] text-slate-400">Spectral Centroid</p>
                        <p className="text-sm font-mono font-semibold text-slate-200 mt-0.5">
                          {analysisResult.audio_metadata?.spectral_centroid_hz?.toFixed(1) || 'N/A'} Hz
                        </p>
                      </div>

                      <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800/80">
                        <p className="text-[11px] text-slate-400">Spectral Flatness</p>
                        <p className="text-sm font-mono font-semibold text-slate-200 mt-0.5">
                          {analysisResult.audio_metadata?.spectral_flatness?.toFixed(4) || 'N/A'}
                        </p>
                      </div>

                      <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800/80">
                        <p className="text-[11px] text-slate-400">Zero Crossing Rate</p>
                        <p className="text-sm font-mono font-semibold text-slate-200 mt-0.5">
                          {analysisResult.audio_metadata?.zero_crossing_rate?.toFixed(4) || 'N/A'}
                        </p>
                      </div>

                      <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800/80">
                        <p className="text-[11px] text-slate-400">High-Freq Energy</p>
                        <p className="text-sm font-mono font-semibold text-slate-200 mt-0.5">
                          {analysisResult.audio_metadata?.high_freq_energy_ratio !== undefined
                            ? `${(analysisResult.audio_metadata.high_freq_energy_ratio * 100).toFixed(1)}%`
                            : 'N/A'}
                        </p>
                      </div>

                      <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800/80">
                        <p className="text-[11px] text-slate-400">VAD Speech Ratio</p>
                        <p className="text-sm font-mono font-semibold text-slate-200 mt-0.5">
                          {analysisResult.audio_metadata?.vad_speech_ratio !== undefined
                            ? `${(analysisResult.audio_metadata.vad_speech_ratio * 100).toFixed(1)}%`
                            : 'N/A'}
                        </p>
                      </div>

                      <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800/80">
                        <p className="text-[11px] text-slate-400">Inference Latency</p>
                        <p className="text-sm font-mono font-semibold text-cyan-400 mt-0.5 flex items-center gap-1">
                          <Clock className="w-3.5 h-3.5" />
                          {analysisResult.inference_time_ms ? `${analysisResult.inference_time_ms} ms` : 'N/A'}
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Toggle JSON view */}
                  <div className="pt-2 flex justify-end">
                    <button
                      type="button"
                      onClick={() => setShowJsonPayload(!showJsonPayload)}
                      className="text-xs text-cyan-400 hover:text-cyan-300 font-mono flex items-center gap-1 cursor-pointer"
                    >
                      <Terminal className="w-3.5 h-3.5" />
                      {showJsonPayload ? 'Hide JSON Response' : 'View REST API JSON Response'}
                    </button>
                  </div>

                  {showJsonPayload && (
                    <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 font-mono text-xs text-slate-300 overflow-x-auto">
                      <pre>{JSON.stringify(analysisResult, null, 2)}</pre>
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="h-full min-h-[350px] p-8 rounded-2xl bg-slate-900/40 border border-slate-800/80 flex flex-col items-center justify-center text-center">
                <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 text-slate-500 mb-4">
                  <Volume2 className="w-8 h-8" />
                </div>
                <h3 className="text-base font-semibold text-slate-300">
                  Awaiting Audio Ingestion
                </h3>
                <p className="text-xs text-slate-500 max-w-sm mt-1">
                  Upload an audio file on the left panel or click one of the quick test sample presets to trigger real-time AI inference.
                </p>
              </div>
            )}
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 bg-slate-950 py-4 px-6 text-center text-xs text-slate-500 font-mono">
        VoiceShield • Smart India Hackathon 2026 (Problem Statement 26104) • AI Voice Integrity Platform
      </footer>
    </div>
  );
}

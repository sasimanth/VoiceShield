/**
 * Shared Type Definitions for VoiceShield Platform
 * Owner: Person 5 (Frontend Engineer)
 */

export interface SpectralBiometrics {
  spectral_centroid_hz: number;
  spectral_flatness: number;
  spectral_rolloff_hz: number;
  zero_crossing_rate: number;
  high_freq_energy_ratio: number;
  acoustic_anomaly_score: number;
}

export interface ProsodicDynamics {
  mean_pitch_f0_hz: number;
  pitch_std_dev: number;
  jitter_percent: number;
  shimmer_percent: number;
  pause_count: number;
  prosodic_anomaly_score: number;
}

export interface SpeakerVerification {
  speaker_id: string;
  enrolled: boolean;
  similarity_score: number | null;
  verified: boolean;
  status: string;
  message?: string;
}

export interface ComplianceLog {
  session_hash: string;
  risk_score: number;
  risk_tier: string;
  raw_audio_stored: boolean;
  retention_policy: string;
  compliance_status: string;
}

export interface AudioAnalysisResponse {
  session_id: string;
  overall_risk_score: number;
  risk_tier: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  action_required: 'ALLOW' | 'CAUTION_PROCEED' | 'STEP_UP_MFA' | 'BLOCK_TRANSACTION';
  recommendations: string[];
  classification: 'BONAFIDE' | 'SPOOF';
  synthetic_probability: number;
  bonafide_probability: number;
  spectral_biometrics: SpectralBiometrics;
  prosodic_dynamics: ProsodicDynamics;
  speaker_verification: SpeakerVerification | null;
  context_threats: string[];
  compliance: ComplianceLog;
}

export interface LiveStreamTelemetry {
  event: string;
  raw_risk: number;
  smoothed_risk: number;
  risk_tier: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  action: string;
  classification: 'BONAFIDE' | 'SPOOF';
  synthetic_prob: number;
  trend: 'STABLE' | 'ESCALATING';
  window_history: number[];
}

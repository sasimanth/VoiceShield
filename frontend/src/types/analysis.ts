export type RiskLevel =
  | "LOW"
  | "MEDIUM"
  | "HIGH"
  | "CRITICAL";

export type AnalysisResult = {
  session_id: string;
  timestamp: string;

  synthetic_probability: number;
  similarity_score: number;

  context_risk: string;

  risk_score: number;
  risk_level: RiskLevel;

  decision: string;

  reasons: string[];

  recommended_action: string;
};
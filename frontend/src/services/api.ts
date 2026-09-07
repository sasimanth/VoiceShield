import type { AnalysisResult } from "../types/analysis";

const API_BASE_URL = "http://localhost:8080/api/v1";

export type AnalyzeAudioParams = {
  file: File;
  sessionId: string;
  claimedSpeakerId: string;
  transactionValueInr: number;
  urgentSocialEngineering: boolean;
  unverifiedBeneficiary: boolean;
};

export async function analyzeAudio({
  file,
  sessionId,
  claimedSpeakerId,
  transactionValueInr,
  urgentSocialEngineering,
  unverifiedBeneficiary,
}: AnalyzeAudioParams): Promise<AnalysisResult> {
  const formData = new FormData();

  formData.append("file", file);
  formData.append("session_id", sessionId);
  formData.append("claimed_speaker_id", claimedSpeakerId);
  formData.append(
    "transaction_value_inr",
    String(transactionValueInr)
  );
  formData.append(
    "urgent_social_engineering",
    String(urgentSocialEngineering)
  );
  formData.append(
    "unverified_beneficiary",
    String(unverifiedBeneficiary)
  );

  const response = await fetch(`${API_BASE_URL}/audio/analyze`, {
    method: "POST",
    headers: {
      // Add this later when Person 3 confirms how your frontend receives the JWT.
      // Authorization: `Bearer ${token}`,
    },
    body: formData,
  });

  if (!response.ok) {
    let message = `Audio analysis failed (${response.status})`;

    try {
      const errorData = await response.json();

      if (errorData?.message) {
        message = errorData.message;
      }
    } catch {
      // Keep the default error message if response isn't JSON.
    }

    throw new Error(message);
  }

  const data: AnalysisResult = await response.json();

  return data;
}
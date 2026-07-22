import type { LogAnalysisResponse, ApiError } from "../types/logAnalysis";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

export class LogAnalysisApiError extends Error {
  status: number;
  errorCode: string;

  constructor(apiError: ApiError) {
    super(apiError.message);
    this.status = apiError.status;
    this.errorCode = apiError.error;
  }
}

export async function analyzeLogFile(file: File): Promise<LogAnalysisResponse> {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE_URL}/api/v1/logs/analyze`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    let apiError: ApiError;
    try {
      apiError = (await response.json()) as ApiError;
    } catch {
      throw new Error("Sunucudan beklenmeyen bir cevap alındı");
    }
    throw new LogAnalysisApiError(apiError);
  }

  return (await response.json()) as LogAnalysisResponse;
}

export async function checkBackendHealth(): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE_URL}/actuator/health`);
    if (!response.ok) {
      return false;
    }
    const data = (await response.json()) as { status?: string };
    return data.status === "UP";
  } catch {
    return false;
  }
}
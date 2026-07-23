import type { LogAnalysisResponse, ApiError } from "../types/logAnalysis";
import type { AnalysisDetail, PagedResponse, AnalysisSummary } from "../types/analysisHistory";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

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

export interface AnalysisHistoryParams {
  page: number;
  size: number;
  sort?: string;
  fileName?: string;
  minErrorCount?: number;
}

export async function fetchAnalysisHistory(params: AnalysisHistoryParams): Promise<PagedResponse<AnalysisSummary>> {
  const query = new URLSearchParams();
  query.set("page", String(params.page));
  query.set("size", String(params.size));
  query.set("sort", params.sort ?? "analyzedAt,desc");
  if (params.fileName) {
    query.set("fileName", params.fileName);
  }
  if (params.minErrorCount !== undefined) {
    query.set("minErrorCount", String(params.minErrorCount));
  }

  const response = await fetch(`${API_BASE_URL}/api/v1/analyses?${query.toString()}`);
  if (!response.ok) {
    throw new Error("Analiz geçmişi yüklenirken bir hata oluştu");
  }
  return (await response.json()) as PagedResponse<AnalysisSummary>;
}

export async function fetchAnalysisDetail(id: number): Promise<AnalysisDetail> {
  const response = await fetch(`${API_BASE_URL}/api/v1/analyses/${id}`);
  if (!response.ok) {
    throw new Error("Analiz detayı yüklenirken bir hata oluştu");
  }
  return (await response.json()) as AnalysisDetail;
}

export async function deleteAnalysis(id: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/v1/analyses/${id}`, { method: "DELETE" });
  if (!response.ok) {
    throw new Error("Analiz kaydı silinirken bir hata oluştu");
  }
}
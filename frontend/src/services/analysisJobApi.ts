import type { ApiError } from "../types/logAnalysis";
import { LogAnalysisApiError } from "./logAnalysisApi";
import type { PagedResponse } from "../types/analysisHistory";
import type {
  CreateAnalysisJobResponse,
  AnalysisJobSummary,
  AnalysisJobDetail,
  JobStatus,
  LogParserType,
} from "../types/analysisJob";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

async function throwApiError(response: Response, fallbackMessage: string): Promise<never> {
  let apiError: ApiError;
  try {
    apiError = (await response.json()) as ApiError;
  } catch {
    throw new Error(fallbackMessage);
  }
  throw new LogAnalysisApiError(apiError);
}

export interface CreateAnalysisJobOptions {
  parserType?: LogParserType;
  startTime?: string;
  endTime?: string;
  levels?: string[];
  logger?: string;
  thread?: string;
  messageContains?: string;
  statusCodes?: string[];
  httpMethods?: string[];
  pathContains?: string;
}

export async function createAnalysisJob(
  file: File,
  analysisName: string,
  options?: CreateAnalysisJobOptions,
): Promise<CreateAnalysisJobResponse> {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("analysisName", analysisName);

  if (options?.parserType) formData.append("parserType", options.parserType);
  if (options?.startTime) formData.append("startTime", options.startTime);
  if (options?.endTime) formData.append("endTime", options.endTime);
  if (options?.levels?.length) formData.append("levels", options.levels.join(","));
  if (options?.logger) formData.append("logger", options.logger);
  if (options?.thread) formData.append("thread", options.thread);
  if (options?.messageContains) formData.append("messageContains", options.messageContains);
  if (options?.statusCodes?.length) formData.append("statusCodes", options.statusCodes.join(","));
  if (options?.httpMethods?.length) formData.append("httpMethods", options.httpMethods.join(","));
  if (options?.pathContains) formData.append("pathContains", options.pathContains);

  const response = await fetch(`${API_BASE_URL}/api/v1/analysis-jobs`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    return throwApiError(response, "Sunucudan beklenmeyen bir cevap alındı");
  }
  return (await response.json()) as CreateAnalysisJobResponse;
}

export async function fetchAnalysisJob(jobId: string): Promise<AnalysisJobDetail> {
  const response = await fetch(`${API_BASE_URL}/api/v1/analysis-jobs/${jobId}`);
  if (!response.ok) {
    return throwApiError(response, "İş detayı yüklenirken bir hata oluştu");
  }
  return (await response.json()) as AnalysisJobDetail;
}

export interface AnalysisJobListParams {
  page: number;
  size: number;
  sort?: string;
  analysisName?: string;
  fileName?: string;
  status?: JobStatus;
}

export async function fetchAnalysisJobs(
  params: AnalysisJobListParams,
): Promise<PagedResponse<AnalysisJobSummary>> {
  const query = new URLSearchParams();
  query.set("page", String(params.page));
  query.set("size", String(params.size));
  query.set("sort", params.sort ?? "createdAt,desc");
  if (params.analysisName) query.set("analysisName", params.analysisName);
  if (params.fileName) query.set("fileName", params.fileName);
  if (params.status) query.set("status", params.status);

  const response = await fetch(`${API_BASE_URL}/api/v1/analysis-jobs?${query.toString()}`);
  if (!response.ok) {
    throw new Error("İş listesi yüklenirken bir hata oluştu");
  }
  return (await response.json()) as PagedResponse<AnalysisJobSummary>;
}

export async function cancelAnalysisJob(jobId: string): Promise<AnalysisJobDetail> {
  const response = await fetch(`${API_BASE_URL}/api/v1/analysis-jobs/${jobId}/cancel`, { method: "POST" });
  if (!response.ok) {
    return throwApiError(response, "İş iptal edilirken bir hata oluştu");
  }
  return (await response.json()) as AnalysisJobDetail;
}

export async function retryAnalysisJob(jobId: string): Promise<AnalysisJobDetail> {
  const response = await fetch(`${API_BASE_URL}/api/v1/analysis-jobs/${jobId}/retry`, { method: "POST" });
  if (!response.ok) {
    return throwApiError(response, "İş yeniden başlatılırken bir hata oluştu");
  }
  return (await response.json()) as AnalysisJobDetail;
}
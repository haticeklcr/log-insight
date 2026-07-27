export type JobStatus = "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELLED";

export interface CreateAnalysisJobResponse {
  jobId: string;
  analysisName: string;
  status: JobStatus;
  progress: number;
  createdAt: string;
}

export interface AnalysisJobSummary {
  jobId: string;
  analysisName: string;
  fileName: string;
  status: JobStatus;
  progress: number;
  retryCount: number;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  analysisId: number | null;
}

export interface AnalysisJobDetail {
  jobId: string;
  analysisName: string;
  fileName: string;
  fileSize: number;
  status: JobStatus;
  progress: number;
  retryCount: number;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  errorCode: string | null;
  analysisId: number | null;
}
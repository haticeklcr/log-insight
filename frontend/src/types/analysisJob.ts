export type JobStatus = "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED" | "CANCELLED";

export type LogParserType =
  | "AUTO"
  | "SPRING_BOOT"
  | "JSON"
  | "NGINX_ACCESS"
  | "APACHE_ACCESS"
  | "PLAIN_TEXT";

export interface AppliedFilters {
  startTime: string | null;
  endTime: string | null;
  levels: string[] | null;
  logger: string | null;
  thread: string | null;
  messageContains: string | null;
  statusCodes: string[] | null;
  httpMethods: string[] | null;
  pathContains: string | null;
}

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
  requestedParserType?: string | null;
  detectedLogFormat?: string | null;
  resumedFromCheckpoint?: boolean;
  appliedFilters?: AppliedFilters | null;
}
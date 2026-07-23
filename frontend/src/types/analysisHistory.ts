import type { ErrorFrequency } from "./logAnalysis";

export interface AnalysisSummary {
  id: number;
  fileName: string;
  fileSize: number;
  analyzedAt: string;
  totalLines: number;
  errorCount: number;
  exceptionCount: number;
  processingDurationMs: number;
}

export interface AnalysisDetail {
  id: number;
  fileName: string;
  fileSize: number;
  analyzedAt: string;
  processingDurationMs: number;
  totalLines: number;
  infoCount: number;
  warningCount: number;
  errorCount: number;
  exceptionCount: number;
  mostFrequentErrors: ErrorFrequency[];
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
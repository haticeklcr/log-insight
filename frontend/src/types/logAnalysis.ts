export interface ErrorFrequency {
  message: string;
  count: number;
}

export interface LogAnalysisResponse {
  fileName: string;
  totalLines: number;
  infoCount: number;
  warningCount: number;
  errorCount: number;
  exceptionCount: number;
  mostFrequentErrors: ErrorFrequency[];
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
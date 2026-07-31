import type { ErrorFrequency } from "./logAnalysis";

export interface AnalysisSummary {
  id: number;
  fileName: string;
  analysisName: string | null;
  fileSize: number;
  analyzedAt: string;
  totalLines: number;
  errorCount: number;
  exceptionCount: number;
  processingDurationMs: number;
}

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

export interface LoggerFrequency {
  loggerName: string;
  count: number;
}

export interface ThreadFrequency {
  threadName: string;
  count: number;
}

export interface StatusCodeCount {
  statusCode: number;
  count: number;
}

export interface HttpMethodCount {
  httpMethod: string;
  count: number;
}

export interface TimelineBucket {
  bucketStart: string;
  totalCount: number;
  infoCount: number;
  warnCount: number;
  errorCount: number;
  exceptionCount: number;
}

export interface AnalysisDetail {
  id: number;
  fileName: string;
  analysisName: string | null;
  fileSize: number;
  analyzedAt: string;
  processingDurationMs: number;
  totalLines: number;
  infoCount: number;
  warningCount: number;
  errorCount: number;
  exceptionCount: number;
  mostFrequentErrors: ErrorFrequency[];
  requestedParserType?: string | null;
  detectedLogFormat?: string | null;
  parsedEntryCount?: number | null;
  unparsedLineCount?: number | null;
  unparsedLinePercentage?: number | null;
  firstLogTimestamp?: string | null;
  lastLogTimestamp?: string | null;
  multilineExceptionCount?: number | null;
  mostFrequentLoggers?: LoggerFrequency[];
  mostFrequentThreads?: ThreadFrequency[];
  statusCodeDistribution?: StatusCodeCount[];
  httpMethodDistribution?: HttpMethodCount[];
  timeline?: TimelineBucket[];
  parseQualityScore?: number | null;
  formatConfidence?: number | null;
  formatDetectionSampleSize?: number | null;
  matchedSampleCount?: number | null;
  appliedFilters?: AppliedFilters | null;
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
export interface CreateUploadSessionResponse {
  uploadId: string;
  chunkSize: number;
  totalChunks: number;
  parallelism: number;
  expiresAt: string;
}

export type UploadSessionStatusValue =
  | "IN_PROGRESS"
  | "MERGING"
  | "COMPLETED"
  | "CONSUMED"
  | "FAILED"
  | "ABORTED";

export interface UploadSessionStatusResponse {
  uploadId: string;
  fileName: string;
  fileSize: number;
  chunkSize: number;
  totalChunks: number;
  receivedCount: number;
  missingChunks: number[];
  status: UploadSessionStatusValue;
  mergeProgress: number;
  expiresAt: string;
}
import type { ApiError } from "../types/logAnalysis";
import { LogAnalysisApiError } from "./logAnalysisApi";
import type { CreateUploadSessionResponse, UploadSessionStatusResponse } from "../types/upload";

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

export async function createUploadSession(
  fileName: string,
  fileSize: number,
): Promise<CreateUploadSessionResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/uploads`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ fileName, fileSize }),
  });
  if (!response.ok) {
    return throwApiError(response, "Yükleme oturumu oluşturulamadı");
  }
  return (await response.json()) as CreateUploadSessionResponse;
}

export async function uploadChunk(uploadId: string, chunkIndex: number, chunk: Blob): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/v1/uploads/${uploadId}/chunks/${chunkIndex}`, {
    method: "PUT",
    headers: { "Content-Type": "application/octet-stream" },
    body: chunk,
  });
  if (response.status === 204) {
    return;
  }
  if (!response.ok) {
    return throwApiError(response, "Parça yüklenemedi");
  }
}

export async function getUploadStatus(uploadId: string): Promise<UploadSessionStatusResponse> {
  const response = await fetch(`${API_BASE_URL}/api/v1/uploads/${uploadId}`);
  if (!response.ok) {
    return throwApiError(response, "Yükleme durumu alınamadı");
  }
  return (await response.json()) as UploadSessionStatusResponse;
}

export async function completeUpload(uploadId: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/v1/uploads/${uploadId}/complete`, { method: "POST" });
  if (response.status === 202) {
    return;
  }
  if (!response.ok) {
    return throwApiError(response, "Yükleme tamamlanamadı");
  }
}

export async function cancelUpload(uploadId: string): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/api/v1/uploads/${uploadId}`, { method: "DELETE" });
  if (response.status === 204) {
    return;
  }
  if (!response.ok) {
    return throwApiError(response, "Yükleme iptal edilemedi");
  }
}
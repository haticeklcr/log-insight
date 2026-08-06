import { useCallback, useRef, useState } from "react";
import { cancelUpload, completeUpload, createUploadSession, getUploadStatus, uploadChunk } from "../services/uploadApi";

export type ChunkedUploadPhase =
  | "idle"
  | "uploading"
  | "merging"
  | "completed"
  | "failed"
  | "cancelled";

export interface ChunkedUploadState {
  phase: ChunkedUploadPhase;
  uploadId: string | null;
  fileName: string | null;
  fileSize: number | null;
  fileLastModified: number | null;
  totalChunks: number;
  uploadedChunks: number;
  mergeProgress: number;
  errorMessage: string | null;
}

export interface PendingUploadInfo {
  uploadId: string;
  fileName: string;
  fileSize: number;
  fileLastModified: number;
}

const STORAGE_KEY = "log-insight-pending-upload";
const MAX_CHUNK_RETRY_ATTEMPTS = 5;
const MERGE_POLL_INTERVAL_MS = 1500;

const IDLE_STATE: ChunkedUploadState = {
  phase: "idle",
  uploadId: null,
  fileName: null,
  fileSize: null,
  fileLastModified: null,
  totalChunks: 0,
  uploadedChunks: 0,
  mergeProgress: 0,
  errorMessage: null,
};

export function loadPendingUpload(): PendingUploadInfo | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as PendingUploadInfo;
  } catch {
    return null;
  }
}

function savePendingUpload(info: PendingUploadInfo) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(info));
}

export function clearPendingUpload() {
  localStorage.removeItem(STORAGE_KEY);
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

export function useChunkedUpload() {
  const [state, setState] = useState<ChunkedUploadState>(IDLE_STATE);
  const cancelledRef = useRef(false);
  const pollTimerRef = useRef<number | null>(null);

  const stopPolling = useCallback(() => {
    if (pollTimerRef.current !== null) {
      window.clearTimeout(pollTimerRef.current);
      pollTimerRef.current = null;
    }
  }, []);

  const uploadChunkWithRetry = useCallback(async (uploadId: string, index: number, blob: Blob) => {
    let attempt = 0;
    // eslint-disable-next-line no-constant-condition
    while (true) {
      try {
        await uploadChunk(uploadId, index, blob);
        return;
      } catch (error) {
        attempt++;
        if (attempt >= MAX_CHUNK_RETRY_ATTEMPTS || cancelledRef.current) {
          throw error;
        }
        await delay(Math.min(1000 * 2 ** attempt, 15000));
      }
    }
  }, []);

  const pollMergeStatus = useCallback((uploadId: string) => {
    const poll = async () => {
      if (cancelledRef.current) return;
      try {
        const status = await getUploadStatus(uploadId);
        if (cancelledRef.current) return;

        if (status.status === "COMPLETED") {
          clearPendingUpload();
          setState((s) => ({ ...s, phase: "completed", mergeProgress: 100 }));
          return;
        }
        if (status.status === "FAILED") {
          setState((s) => ({ ...s, phase: "failed", errorMessage: "mergeFailed" }));
          return;
        }
        setState((s) => ({ ...s, mergeProgress: status.mergeProgress }));
      } catch {
        // ağ hatası — bir sonraki taramada tekrar denenecek, oturumu FAILED yapmıyoruz
      }
      pollTimerRef.current = window.setTimeout(poll, MERGE_POLL_INTERVAL_MS);
    };
    poll();
  }, []);

  const runUpload = useCallback(
    async (file: File, uploadId: string, chunkSize: number, totalChunks: number, parallelism: number,
           missingIndices: number[]) => {
      let uploadedCount = totalChunks - missingIndices.length;
      setState((s) => ({ ...s, uploadId, totalChunks, uploadedChunks: uploadedCount, phase: "uploading" }));

      let cursor = 0;
      const worker = async () => {
        while (cursor < missingIndices.length) {
          if (cancelledRef.current) return;
          const chunkIndex = missingIndices[cursor++];
          const start = chunkIndex * chunkSize;
          const end = Math.min(start + chunkSize, file.size);
          const blob = file.slice(start, end);
          await uploadChunkWithRetry(uploadId, chunkIndex, blob);
          if (cancelledRef.current) return;
          uploadedCount++;
          setState((s) => ({ ...s, uploadedChunks: uploadedCount }));
        }
      };

      const workerCount = Math.max(1, Math.min(parallelism, missingIndices.length || 1));
      try {
        await Promise.all(Array.from({ length: workerCount }, () => worker()));
      } catch (error) {
        if (!cancelledRef.current) {
          setState((s) => ({ ...s, phase: "failed", errorMessage: "uploadFailed" }));
        }
        return;
      }
      if (cancelledRef.current) return;

      try {
        await completeUpload(uploadId);
      } catch {
        setState((s) => ({ ...s, phase: "failed", errorMessage: "uploadFailed" }));
        return;
      }
      setState((s) => ({ ...s, phase: "merging", mergeProgress: 0 }));
      pollMergeStatus(uploadId);
    },
    [uploadChunkWithRetry, pollMergeStatus],
  );

  const start = useCallback(
    async (file: File) => {
      cancelledRef.current = false;
      setState({ ...IDLE_STATE, fileName: file.name, fileSize: file.size, fileLastModified: file.lastModified });
      try {
        const session = await createUploadSession(file.name, file.size);
        savePendingUpload({
          uploadId: session.uploadId,
          fileName: file.name,
          fileSize: file.size,
          fileLastModified: file.lastModified,
        });
        const allIndices = Array.from({ length: session.totalChunks }, (_, i) => i);
        await runUpload(file, session.uploadId, session.chunkSize, session.totalChunks,
                session.parallelism, allIndices);
      } catch {
        setState((s) => ({ ...s, phase: "failed", errorMessage: "uploadFailed" }));
      }
    },
    [runUpload],
  );

  const resume = useCallback(
    async (file: File, pending: PendingUploadInfo) => {
      cancelledRef.current = false;
      setState({
        ...IDLE_STATE,
        uploadId: pending.uploadId,
        fileName: file.name,
        fileSize: file.size,
        fileLastModified: file.lastModified,
      });
      try {
        const status = await getUploadStatus(pending.uploadId);
        await runUpload(file, pending.uploadId, status.chunkSize, status.totalChunks,
                4, status.missingChunks);
      } catch {
        clearPendingUpload();
        setState((s) => ({ ...s, phase: "failed", errorMessage: "uploadFailed" }));
      }
    },
    [runUpload],
  );

  const cancel = useCallback(async () => {
    cancelledRef.current = true;
    stopPolling();
    const uploadId = state.uploadId;
    clearPendingUpload();
    setState({ ...IDLE_STATE, phase: "cancelled" });
    if (uploadId) {
      try {
        await cancelUpload(uploadId);
      } catch {
        // iptal isteği başarısız olsa bile kullanıcı arayüzü zaten sıfırlandı
      }
    }
  }, [state.uploadId, stopPolling]);

  const reset = useCallback(() => {
    stopPolling();
    cancelledRef.current = false;
    setState(IDLE_STATE);
  }, [stopPolling]);

  return { state, start, resume, cancel, reset };
}
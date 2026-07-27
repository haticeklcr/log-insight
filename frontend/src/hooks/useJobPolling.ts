import { useCallback, useEffect, useRef, useState } from "react";
import { fetchAnalysisJob } from "../services/analysisJobApi";
import type { AnalysisJobDetail, JobStatus } from "../types/analysisJob";

const POLL_INTERVAL_MS = 2000;
const TERMINAL_STATUSES: JobStatus[] = ["SUCCEEDED", "FAILED", "CANCELLED"];

export function useJobPolling(jobId: string | null) {
  const [job, setJob] = useState<AnalysisJobDetail | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const jobIdRef = useRef(jobId);

  const poll = useCallback(async () => {
    const currentJobId = jobIdRef.current;
    if (!currentJobId) return;
    try {
      const latest = await fetchAnalysisJob(currentJobId);
      setJob(latest);
      setErrorMessage(null);
      if (TERMINAL_STATUSES.includes(latest.status) && timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    } catch {
      setErrorMessage("Backend servisine ulaşılamadı. İş durumu güncellenemiyor.");
    }
  }, []);

  useEffect(() => {
    jobIdRef.current = jobId;
    setJob(null);
    setErrorMessage(null);

    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }

    if (!jobId) {
      return;
    }

    poll();
    timerRef.current = setInterval(poll, POLL_INTERVAL_MS);

    return () => {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }
    };
  }, [jobId, poll]);

  return { job, errorMessage, refetch: poll };
}
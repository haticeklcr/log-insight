import { useCallback, useEffect, useState } from "react";
import styles from "./JobsListView.module.css";
import JobsTable from "../JobsTable/JobsTable";
import JobFilterBar from "../JobFilterBar/JobFilterBar";
import Pagination from "../Pagination/Pagination";
import LoadingIndicator from "../LoadingIndicator/LoadingIndicator";
import ErrorAlert from "../ErrorAlert/ErrorAlert";
import { fetchAnalysisJobs, cancelAnalysisJob, retryAnalysisJob } from "../../services/analysisJobApi";
import type { AnalysisJobSummary, JobStatus } from "../../types/analysisJob";
import type { PagedResponse } from "../../types/analysisHistory";

interface JobsListViewProps {
  onViewDetail: (jobId: string) => void;
}

const PAGE_SIZE = 10;

export default function JobsListView({ onViewDetail }: JobsListViewProps) {
  const [page, setPage] = useState(0);
  const [analysisNameFilter, setAnalysisNameFilter] = useState("");
  const [fileNameFilter, setFileNameFilter] = useState("");
  const [statusFilter, setStatusFilter] = useState<JobStatus | "">("");
  const [data, setData] = useState<PagedResponse<AnalysisJobSummary> | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const response = await fetchAnalysisJobs({
        page,
        size: PAGE_SIZE,
        analysisName: analysisNameFilter || undefined,
        fileName: fileNameFilter || undefined,
        status: statusFilter || undefined,
      });
      setData(response);
    } catch {
      setErrorMessage("İş listesi yüklenirken bir hata oluştu. Backend servisine ulaşılamıyor olabilir.");
    } finally {
      setIsLoading(false);
    }
  }, [page, analysisNameFilter, fileNameFilter, statusFilter]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  function handleApplyFilters(analysisName: string, fileName: string, status: JobStatus | "") {
    setAnalysisNameFilter(analysisName);
    setFileNameFilter(fileName);
    setStatusFilter(status);
    setPage(0);
  }

  async function handleCancel(jobId: string) {
    try {
      await cancelAnalysisJob(jobId);
      await loadData();
    } catch {
      setErrorMessage("İş iptal edilirken bir hata oluştu");
    }
  }

  async function handleRetry(jobId: string) {
    try {
      await retryAnalysisJob(jobId);
      await loadData();
    } catch {
      setErrorMessage("İş yeniden başlatılırken bir hata oluştu");
    }
  }

  return (
    <div className={styles.container}>
      <JobFilterBar onApply={handleApplyFilters} />

      {isLoading && <LoadingIndicator />}
      {errorMessage && <ErrorAlert message={errorMessage} />}

      {!isLoading && !errorMessage && data && data.content.length === 0 && (
        <p className={styles.empty}>Henüz analiz işi bulunmuyor.</p>
      )}

      {!isLoading && !errorMessage && data && data.content.length > 0 && (
        <>
          <JobsTable jobs={data.content} onViewDetail={onViewDetail} onCancel={handleCancel} onRetry={handleRetry} />
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            first={data.first}
            last={data.last}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  );
}
import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import styles from "./JobsListView.module.css";
import JobsTable from "../JobsTable/JobsTable";
import JobFilterBar from "../JobFilterBar/JobFilterBar";
import Pagination from "../Pagination/Pagination";
import LoadingIndicator from "../LoadingIndicator/LoadingIndicator";
import ErrorAlert from "../ErrorAlert/ErrorAlert";
import { fetchAnalysisJobs, cancelAnalysisJob, retryAnalysisJob } from "../../services/analysisJobApi";
import { translateApiError } from "../../utils/apiErrorMessage";
import type { AnalysisJobSummary, JobStatus } from "../../types/analysisJob";
import type { PagedResponse } from "../../types/analysisHistory";

interface JobsListViewProps {
  onViewDetail: (jobId: string) => void;
}

const PAGE_SIZE = 10;

export default function JobsListView({ onViewDetail }: JobsListViewProps) {
  const { t } = useTranslation();
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
      setErrorMessage(t("jobsListView.loadError"));
    } finally {
      setIsLoading(false);
    }
  }, [page, analysisNameFilter, fileNameFilter, statusFilter, t]);

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
    } catch (error) {
      setErrorMessage(translateApiError(error, t, "jobsListView.cancelError"));
    }
  }

  async function handleRetry(jobId: string) {
    try {
      await retryAnalysisJob(jobId);
      await loadData();
    } catch (error) {
      setErrorMessage(translateApiError(error, t, "jobsListView.retryError"));
    }
  }

  return (
    <div className={styles.container}>
      <JobFilterBar onApply={handleApplyFilters} />

      {isLoading && <LoadingIndicator />}
      {errorMessage && <ErrorAlert message={errorMessage} />}

      {!isLoading && !errorMessage && data && data.content.length === 0 && (
        <p className={styles.empty}>{t("jobsListView.empty")}</p>
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
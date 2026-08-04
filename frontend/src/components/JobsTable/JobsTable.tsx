import { useTranslation } from "react-i18next";
import styles from "./JobsTable.module.css";
import JobStatusBadge from "../JobStatusBadge/JobStatusBadge";
import type { AnalysisJobSummary, JobStatus } from "../../types/analysisJob";

interface JobsTableProps {
  jobs: AnalysisJobSummary[];
  onViewDetail: (jobId: string) => void;
  onCancel: (jobId: string) => void;
  onRetry: (jobId: string) => void;
  analysisNameFilter: string;
  fileNameFilter: string;
  statusFilter: JobStatus | "";
  onAnalysisNameFilterChange: (value: string) => void;
  onFileNameFilterChange: (value: string) => void;
  onStatusFilterChange: (value: JobStatus | "") => void;
  onApplyFilters: () => void;
}

const STATUS_VALUES: (JobStatus | "")[] = ["", "PENDING", "RUNNING", "SUCCEEDED", "FAILED", "CANCELLED"];

function formatDateTime(value: string | null, locale: string): string {
  if (!value) return "—";
  return new Date(value).toLocaleString(locale);
}

export default function JobsTable({
  jobs,
  onViewDetail,
  onCancel,
  onRetry,
  analysisNameFilter,
  fileNameFilter,
  statusFilter,
  onAnalysisNameFilterChange,
  onFileNameFilterChange,
  onStatusFilterChange,
  onApplyFilters,
}: JobsTableProps) {
  const { t, i18n } = useTranslation();
  const locale = i18n.language === "en" ? "en-US" : "tr-TR";

  function handleEnterKey(event: React.KeyboardEvent) {
    if (event.key === "Enter") {
      onApplyFilters();
    }
  }

  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <th className={styles.headerCell}>{t("jobsTable.analysisName")}</th>
          <th className={styles.headerCell}>{t("jobsTable.fileName")}</th>
          <th className={styles.headerCell}>{t("jobsTable.status")}</th>
          <th className={styles.headerCell}>{t("jobsTable.progress")}</th>
          <th className={styles.headerCell}>{t("jobsTable.createdAt")}</th>
          <th className={styles.headerCell}>{t("jobsTable.startedAt")}</th>
          <th className={styles.headerCell}>{t("jobsTable.completedAt")}</th>
          <th className={styles.headerCell}>{t("jobsTable.retryCount")}</th>
          <th className={styles.headerCell}>{t("jobsTable.actions")}</th>
        </tr>
        <tr className={styles.filterRow}>
          <th className={styles.filterCell}>
            <input
              type="text"
              placeholder={t("jobFilters.analysisNamePlaceholder")}
              value={analysisNameFilter}
              onChange={(e) => onAnalysisNameFilterChange(e.target.value)}
              onKeyDown={handleEnterKey}
              className={styles.filterInput}
            />
          </th>
          <th className={styles.filterCell}>
            <input
              type="text"
              placeholder={t("jobFilters.fileNamePlaceholder")}
              value={fileNameFilter}
              onChange={(e) => onFileNameFilterChange(e.target.value)}
              onKeyDown={handleEnterKey}
              className={styles.filterInput}
            />
          </th>
          <th className={styles.filterCell}>
            <select
              value={statusFilter}
              onChange={(e) => onStatusFilterChange(e.target.value as JobStatus | "")}
              className={styles.filterInput}
            >
              {STATUS_VALUES.map((value) => (
                <option key={value} value={value}>
                  {value === "" ? t("jobFilters.allStatuses") : t(`jobStatus.${value}`)}
                </option>
              ))}
            </select>
          </th>
          <th className={styles.filterCell} colSpan={5} />
          <th className={styles.filterCell}>
            <button type="button" className={styles.applyButton} onClick={onApplyFilters}>
              {t("jobFilters.apply")}
            </button>
          </th>
        </tr>
      </thead>
      <tbody>
        {jobs.map((job) => {
          const canCancel = job.status === "PENDING" || job.status === "RUNNING";
          const canRetry = job.status === "FAILED";
          const canViewResult = job.status === "SUCCEEDED" && job.analysisId !== null;
          return (
            <tr key={job.jobId}>
              <td className={styles.cell}>{job.analysisName}</td>
              <td className={styles.cell}>{job.fileName}</td>
              <td className={styles.cell}>
                <JobStatusBadge status={job.status} />
              </td>
              <td className={styles.cell}>%{job.progress}</td>
              <td className={styles.cell}>{formatDateTime(job.createdAt, locale)}</td>
              <td className={styles.cell}>{formatDateTime(job.startedAt, locale)}</td>
              <td className={styles.cell}>{formatDateTime(job.completedAt, locale)}</td>
              <td className={styles.cell}>{job.retryCount}</td>
              <td className={styles.cell}>
                <button type="button" className={styles.actionButton} onClick={() => onViewDetail(job.jobId)}>
                  {t("jobsTable.detail")}
                </button>
                <button
                  type="button"
                  className={styles.actionButton}
                  onClick={() => onCancel(job.jobId)}
                  disabled={!canCancel}
                >
                  {t("jobsTable.cancel")}
                </button>
                <button
                  type="button"
                  className={styles.actionButton}
                  onClick={() => onRetry(job.jobId)}
                  disabled={!canRetry}
                >
                  {t("jobsTable.retry")}
                </button>
                <button
                  type="button"
                  className={styles.actionButton}
                  onClick={() => onViewDetail(job.jobId)}
                  disabled={!canViewResult}
                >
                  {t("jobsTable.result")}
                </button>
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
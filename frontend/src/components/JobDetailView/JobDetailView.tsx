import { useTranslation } from "react-i18next";
import styles from "./JobDetailView.module.css";
import JobStatusBadge from "../JobStatusBadge/JobStatusBadge";
import type { AnalysisJobDetail } from "../../types/analysisJob";

interface JobDetailViewProps {
  job: AnalysisJobDetail;
  pollingErrorMessage: string | null;
  onCancel: () => void;
  onRetry: () => void;
  onViewResult: (analysisId: number) => void;
  onBack: () => void;
}

export default function JobDetailView({
  job,
  pollingErrorMessage,
  onCancel,
  onRetry,
  onViewResult,
  onBack,
}: JobDetailViewProps) {
  const { t, i18n } = useTranslation();
  const locale = i18n.language === "en" ? "en-US" : "tr-TR";

  const canCancel = job.status === "PENDING" || job.status === "RUNNING";
  const canRetry = job.status === "FAILED";
  const canViewResult = job.status === "SUCCEEDED" && job.analysisId !== null;
  const showProgressBar = job.status === "PENDING" || job.status === "RUNNING";

  function formatDateTime(value: string | null): string {
    if (!value) return t("jobDetail.notAvailable");
    return new Date(value).toLocaleString(locale);
  }

  function formatDuration(startedAt: string | null, completedAt: string | null): string {
    if (!startedAt) return t("jobDetail.notAvailable");
    const start = new Date(startedAt).getTime();
    const end = completedAt ? new Date(completedAt).getTime() : Date.now();
    const seconds = Math.max(0, Math.round((end - start) / 1000));
    if (seconds < 60) return `${seconds} ${t("jobDetail.seconds")}`;
    return `${Math.floor(seconds / 60)} ${t("jobDetail.minutes")} ${seconds % 60} ${t("jobDetail.seconds")}`;
  }

  function errorMessageFor(errorCode: string | null): string {
    if (!errorCode) return t("errors.generic");
    const key = `errors.${errorCode}`;
    const translated = t(key);
    return translated === key ? t("errors.generic") : translated;
  }

  return (
    <div className={styles.container}>
      <button type="button" className={styles.backButton} onClick={onBack}>
        {t("jobDetail.back")}
      </button>

      <div className={styles.titleRow}>
        <h2 className={styles.title}>{job.analysisName}</h2>
        <JobStatusBadge status={job.status} />
      </div>

      {job.resumedFromCheckpoint && (
        <p className={styles.resumedNote}>{t("jobDetail.resumedFromCheckpoint")}</p>
      )}
      {!job.resumedFromCheckpoint && job.retryCount > 0 && (
        <p className={styles.resumedNote}>{t("jobDetail.restartedFromScratch")}</p>
      )}

      {pollingErrorMessage && <p className={styles.pollingWarning}>{pollingErrorMessage}</p>}

      {showProgressBar && (
        <div className={styles.progressBarOuter}>
          <div className={styles.progressBarInner} style={{ width: `${job.progress}%` }} />
          <span className={styles.progressLabel}>%{job.progress}</span>
        </div>
      )}

      <dl className={styles.detailList}>
        <dt>{t("jobDetail.jobId")}</dt>
        <dd>{job.jobId}</dd>
        <dt>{t("jobDetail.fileName")}</dt>
        <dd>{job.fileName}</dd>
        <dt>{t("jobDetail.fileSize")}</dt>
        <dd>{job.fileSize} {t("jobDetail.bytes")}</dd>
        <dt>{t("jobDetail.createdAt")}</dt>
        <dd>{formatDateTime(job.createdAt)}</dd>
        <dt>{t("jobDetail.startedAt")}</dt>
        <dd>{formatDateTime(job.startedAt)}</dd>
        <dt>{t("jobDetail.completedAt")}</dt>
        <dd>{formatDateTime(job.completedAt)}</dd>
        <dt>{t("jobDetail.elapsed")}</dt>
        <dd>{formatDuration(job.startedAt, job.completedAt)}</dd>
        <dt>{t("jobDetail.retryCount")}</dt>
        <dd>{job.retryCount}</dd>
      </dl>

      {job.status === "FAILED" && job.errorCode && (
        <p className={styles.errorInfo}>{errorMessageFor(job.errorCode)}</p>
      )}

      <div className={styles.actions}>
        {canCancel && (
          <button type="button" className={styles.cancelButton} onClick={onCancel}>
            {t("jobDetail.cancel")}
          </button>
        )}
        {canRetry && (
          <button type="button" className={styles.retryButton} onClick={onRetry}>
            {t("jobDetail.retry")}
          </button>
        )}
        {canViewResult && (
          <button
            type="button"
            className={styles.resultButton}
            onClick={() => onViewResult(job.analysisId as number)}
          >
            {t("jobDetail.viewResult")}
          </button>
        )}
      </div>
    </div>
  );
}
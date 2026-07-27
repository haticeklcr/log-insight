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

const ERROR_CODE_MESSAGES: Record<string, string> = {
  APPLICATION_RESTARTED_DURING_ANALYSIS: "Uygulama yeniden başlatıldığı için bu analiz yarıda kaldı.",
  ANALYSIS_IO_ERROR: "Log dosyası okunurken bir hata oluştu.",
  ANALYSIS_UNEXPECTED_ERROR: "Analiz sırasında beklenmeyen bir hata oluştu.",
};

function formatDateTime(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toLocaleString("tr-TR");
}

function formatDuration(startedAt: string | null, completedAt: string | null): string {
  if (!startedAt) return "—";
  const start = new Date(startedAt).getTime();
  const end = completedAt ? new Date(completedAt).getTime() : Date.now();
  const seconds = Math.max(0, Math.round((end - start) / 1000));
  if (seconds < 60) return `${seconds} sn`;
  return `${Math.floor(seconds / 60)} dk ${seconds % 60} sn`;
}

export default function JobDetailView({
  job,
  pollingErrorMessage,
  onCancel,
  onRetry,
  onViewResult,
  onBack,
}: JobDetailViewProps) {
  const canCancel = job.status === "PENDING" || job.status === "RUNNING";
  const canRetry = job.status === "FAILED";
  const canViewResult = job.status === "SUCCEEDED" && job.analysisId !== null;
  const showProgressBar = job.status === "PENDING" || job.status === "RUNNING";

  return (
    <div className={styles.container}>
      <button type="button" className={styles.backButton} onClick={onBack}>
        ← Geri
      </button>

      <div className={styles.titleRow}>
        <h2 className={styles.title}>{job.analysisName}</h2>
        <JobStatusBadge status={job.status} />
      </div>

      {pollingErrorMessage && <p className={styles.pollingWarning}>{pollingErrorMessage}</p>}

      {showProgressBar && (
        <div className={styles.progressBarOuter}>
          <div className={styles.progressBarInner} style={{ width: `${job.progress}%` }} />
          <span className={styles.progressLabel}>%{job.progress}</span>
        </div>
      )}

      <dl className={styles.detailList}>
        <dt>İş ID</dt>
        <dd>{job.jobId}</dd>
        <dt>Dosya Adı</dt>
        <dd>{job.fileName}</dd>
        <dt>Dosya Boyutu</dt>
        <dd>{job.fileSize} bayt</dd>
        <dt>Oluşturulma Zamanı</dt>
        <dd>{formatDateTime(job.createdAt)}</dd>
        <dt>Başlama Zamanı</dt>
        <dd>{formatDateTime(job.startedAt)}</dd>
        <dt>Tamamlanma Zamanı</dt>
        <dd>{formatDateTime(job.completedAt)}</dd>
        <dt>Geçen Süre</dt>
        <dd>{formatDuration(job.startedAt, job.completedAt)}</dd>
        <dt>Retry Sayısı</dt>
        <dd>{job.retryCount}</dd>
      </dl>

      {job.status === "FAILED" && job.errorCode && (
        <p className={styles.errorInfo}>
          {ERROR_CODE_MESSAGES[job.errorCode] ?? "Analiz başarısız oldu."}
        </p>
      )}

      <div className={styles.actions}>
        {canCancel && (
          <button type="button" className={styles.cancelButton} onClick={onCancel}>
            İptal Et
          </button>
        )}
        {canRetry && (
          <button type="button" className={styles.retryButton} onClick={onRetry}>
            Tekrar Dene
          </button>
        )}
        {canViewResult && (
          <button
            type="button"
            className={styles.resultButton}
            onClick={() => onViewResult(job.analysisId as number)}
          >
            Sonucu Görüntüle
          </button>
        )}
      </div>
    </div>
  );
}
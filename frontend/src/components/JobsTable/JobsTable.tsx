import styles from "./JobsTable.module.css";
import JobStatusBadge from "../JobStatusBadge/JobStatusBadge";
import type { AnalysisJobSummary } from "../../types/analysisJob";

interface JobsTableProps {
  jobs: AnalysisJobSummary[];
  onViewDetail: (jobId: string) => void;
  onCancel: (jobId: string) => void;
  onRetry: (jobId: string) => void;
}

function formatDateTime(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toLocaleString("tr-TR");
}

export default function JobsTable({ jobs, onViewDetail, onCancel, onRetry }: JobsTableProps) {
  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <th className={styles.headerCell}>Analiz Adı</th>
          <th className={styles.headerCell}>Dosya Adı</th>
          <th className={styles.headerCell}>Durum</th>
          <th className={styles.headerCell}>İlerleme</th>
          <th className={styles.headerCell}>Oluşturulma</th>
          <th className={styles.headerCell}>Başlama</th>
          <th className={styles.headerCell}>Tamamlanma</th>
          <th className={styles.headerCell}>Retry</th>
          <th className={styles.headerCell}>İşlemler</th>
        </tr>
      </thead>
      <tbody>
        {jobs.map((job) => {
          const canCancel = job.status === "PENDING" || job.status === "RUNNING";
          const canRetry = job.status === "FAILED";
          const canViewResult = job.status === "SUCCEEDED";
          return (
            <tr key={job.jobId}>
              <td className={styles.cell}>{job.analysisName}</td>
              <td className={styles.cell}>{job.fileName}</td>
              <td className={styles.cell}>
                <JobStatusBadge status={job.status} />
              </td>
              <td className={styles.cell}>%{job.progress}</td>
              <td className={styles.cell}>{formatDateTime(job.createdAt)}</td>
              <td className={styles.cell}>{formatDateTime(job.startedAt)}</td>
              <td className={styles.cell}>{formatDateTime(job.completedAt)}</td>
              <td className={styles.cell}>{job.retryCount}</td>
              <td className={styles.cell}>
                <button type="button" className={styles.actionButton} onClick={() => onViewDetail(job.jobId)}>
                  Detay
                </button>
                <button
                  type="button"
                  className={styles.actionButton}
                  onClick={() => onCancel(job.jobId)}
                  disabled={!canCancel}
                >
                  İptal
                </button>
                <button
                  type="button"
                  className={styles.actionButton}
                  onClick={() => onRetry(job.jobId)}
                  disabled={!canRetry}
                >
                  Retry
                </button>
                <button
                  type="button"
                  className={styles.actionButton}
                  onClick={() => onViewDetail(job.jobId)}
                  disabled={!canViewResult}
                >
                  Sonuç
                </button>
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
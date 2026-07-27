import styles from "./JobStatusBadge.module.css";
import type { JobStatus } from "../../types/analysisJob";

const LABELS: Record<JobStatus, string> = {
  PENDING: "Bekliyor",
  RUNNING: "Çalışıyor",
  SUCCEEDED: "Tamamlandı",
  FAILED: "Başarısız",
  CANCELLED: "İptal Edildi",
};

const STYLE_KEYS: Record<JobStatus, string> = {
  PENDING: "pending",
  RUNNING: "running",
  SUCCEEDED: "succeeded",
  FAILED: "failed",
  CANCELLED: "cancelled",
};

interface JobStatusBadgeProps {
  status: JobStatus;
}

export default function JobStatusBadge({ status }: JobStatusBadgeProps) {
  return <span className={`${styles.badge} ${styles[STYLE_KEYS[status]]}`}>{LABELS[status]}</span>;
}
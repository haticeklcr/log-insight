import { useTranslation } from "react-i18next";
import styles from "./JobStatusBadge.module.css";
import type { JobStatus } from "../../types/analysisJob";

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
  const { t } = useTranslation();
  return (
    <span className={`${styles.badge} ${styles[STYLE_KEYS[status]]}`}>
      {t(`jobStatus.${status}`)}
    </span>
  );
}
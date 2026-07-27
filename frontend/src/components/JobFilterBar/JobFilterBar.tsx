import { useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import styles from "./JobFilterBar.module.css";
import type { JobStatus } from "../../types/analysisJob";

interface JobFilterBarProps {
  onApply: (analysisName: string, fileName: string, status: JobStatus | "") => void;
}

const STATUS_VALUES: (JobStatus | "")[] = ["", "PENDING", "RUNNING", "SUCCEEDED", "FAILED", "CANCELLED"];

export default function JobFilterBar({ onApply }: JobFilterBarProps) {
  const { t } = useTranslation();
  const [analysisName, setAnalysisName] = useState("");
  const [fileName, setFileName] = useState("");
  const [status, setStatus] = useState<JobStatus | "">("");

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onApply(analysisName, fileName, status);
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <input
        type="text"
        placeholder={t("jobFilters.analysisNamePlaceholder")}
        value={analysisName}
        onChange={(e) => setAnalysisName(e.target.value)}
        className={styles.input}
      />
      <input
        type="text"
        placeholder={t("jobFilters.fileNamePlaceholder")}
        value={fileName}
        onChange={(e) => setFileName(e.target.value)}
        className={styles.input}
      />
      <select
        value={status}
        onChange={(e) => setStatus(e.target.value as JobStatus | "")}
        className={styles.select}
      >
        {STATUS_VALUES.map((value) => (
          <option key={value} value={value}>
            {value === "" ? t("jobFilters.allStatuses") : t(`jobStatus.${value}`)}
          </option>
        ))}
      </select>
      <button type="submit" className={styles.button}>
        {t("jobFilters.apply")}
      </button>
    </form>
  );
}
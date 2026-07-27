import { useState } from "react";
import type { FormEvent } from "react";
import styles from "./JobFilterBar.module.css";
import type { JobStatus } from "../../types/analysisJob";

interface JobFilterBarProps {
  onApply: (analysisName: string, fileName: string, status: JobStatus | "") => void;
}

const STATUS_OPTIONS: { value: JobStatus | ""; label: string }[] = [
  { value: "", label: "Tüm Durumlar" },
  { value: "PENDING", label: "Bekliyor" },
  { value: "RUNNING", label: "Çalışıyor" },
  { value: "SUCCEEDED", label: "Tamamlandı" },
  { value: "FAILED", label: "Başarısız" },
  { value: "CANCELLED", label: "İptal Edildi" },
];

export default function JobFilterBar({ onApply }: JobFilterBarProps) {
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
        placeholder="Analiz adına göre ara"
        value={analysisName}
        onChange={(e) => setAnalysisName(e.target.value)}
        className={styles.input}
      />
      <input
        type="text"
        placeholder="Dosya adına göre ara"
        value={fileName}
        onChange={(e) => setFileName(e.target.value)}
        className={styles.input}
      />
      <select
        value={status}
        onChange={(e) => setStatus(e.target.value as JobStatus | "")}
        className={styles.select}
      >
        {STATUS_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <button type="submit" className={styles.button}>
        Uygula
      </button>
    </form>
  );
}
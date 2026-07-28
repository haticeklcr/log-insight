import { useState } from "react";
import type { FormEvent } from "react";
import { useTranslation } from "react-i18next";
import styles from "./SearchFilterBar.module.css";

interface SearchFilterBarProps {
  onApply: (fileName: string, analysisName: string, minErrorCount: string) => void;
}

export default function SearchFilterBar({ onApply }: SearchFilterBarProps) {
  const { t } = useTranslation();
  const [fileName, setFileName] = useState("");
  const [analysisName, setAnalysisName] = useState("");
  const [minErrorCount, setMinErrorCount] = useState("");

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onApply(fileName, analysisName, minErrorCount);
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <input
        type="text"
        placeholder={t("searchFilterBar.analysisNamePlaceholder")}
        value={analysisName}
        onChange={(e) => setAnalysisName(e.target.value)}
        className={styles.input}
      />
      <input
        type="text"
        placeholder={t("searchFilterBar.fileNamePlaceholder")}
        value={fileName}
        onChange={(e) => setFileName(e.target.value)}
        className={styles.input}
      />
      <input
        type="number"
        min={0}
        placeholder={t("searchFilterBar.minErrorPlaceholder")}
        value={minErrorCount}
        onChange={(e) => setMinErrorCount(e.target.value)}
        className={styles.input}
      />
      <button type="submit" className={styles.button}>
        {t("searchFilterBar.apply")}
      </button>
    </form>
  );
}
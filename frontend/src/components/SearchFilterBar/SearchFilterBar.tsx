import { useState } from "react";
import type { FormEvent } from "react";
import styles from "./SearchFilterBar.module.css";

interface SearchFilterBarProps {
  onApply: (fileName: string, minErrorCount: string) => void;
}

export default function SearchFilterBar({ onApply }: SearchFilterBarProps) {
  const [fileName, setFileName] = useState("");
  const [minErrorCount, setMinErrorCount] = useState("");

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onApply(fileName, minErrorCount);
  }

  return (
    <form className={styles.form} onSubmit={handleSubmit}>
      <input
        type="text"
        placeholder="Dosya adına göre ara"
        value={fileName}
        onChange={(e) => setFileName(e.target.value)}
        className={styles.input}
      />
      <input
        type="number"
        min={0}
        placeholder="Min. ERROR sayısı"
        value={minErrorCount}
        onChange={(e) => setMinErrorCount(e.target.value)}
        className={styles.input}
      />
      <button type="submit" className={styles.button}>
        Uygula
      </button>
    </form>
  );
}
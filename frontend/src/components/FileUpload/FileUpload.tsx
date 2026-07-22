import { useRef, useState } from "react";
import type { ChangeEvent, DragEvent } from "react";
import styles from "./FileUpload.module.css";

interface FileUploadProps {
  onAnalyze: (file: File) => void;
  isLoading: boolean;
}

const ALLOWED_EXTENSIONS = [".log", ".txt"];

function isAllowedFile(file: File): boolean {
  const lowerName = file.name.toLowerCase();
  return ALLOWED_EXTENSIONS.some((ext) => lowerName.endsWith(ext));
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function FileUpload({ onAnalyze, isLoading }: FileUploadProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [isDragActive, setIsDragActive] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  function handleFile(file: File) {
    if (!isAllowedFile(file)) {
      setValidationError("Yalnızca .log ve .txt dosyaları seçilebilir");
      setSelectedFile(null);
      return;
    }
    setValidationError(null);
    setSelectedFile(file);
  }

  function handleInputChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (file) {
      handleFile(file);
    }
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setIsDragActive(false);
    const file = event.dataTransfer.files?.[0];
    if (file) {
      handleFile(file);
    }
  }

  function handleDragOver(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setIsDragActive(true);
  }

  function handleDragLeave() {
    setIsDragActive(false);
  }

  function handleAnalyzeClick() {
    if (!selectedFile) {
      setValidationError("Lütfen önce bir dosya seçin");
      return;
    }
    onAnalyze(selectedFile);
  }

  return (
    <div className={styles.container}>
      <div
        className={`${styles.dropzone} ${isDragActive ? styles.dropzoneActive : ""}`}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
      >
        <p className={styles.dropzoneText}>Dosyayı buraya sürükleyin veya seçin</p>
        <button
          type="button"
          className={styles.selectButton}
          onClick={() => inputRef.current?.click()}
          disabled={isLoading}
        >
          Dosya Seç
        </button>
        <input
          ref={inputRef}
          type="file"
          accept=".log,.txt"
          className={styles.hiddenInput}
          onChange={handleInputChange}
          disabled={isLoading}
        />
      </div>

      {selectedFile && (
        <div className={styles.fileInfo}>
          <span className={styles.fileName}>{selectedFile.name}</span>
          <span className={styles.fileSize}>{formatFileSize(selectedFile.size)}</span>
        </div>
      )}

      {validationError && <p className={styles.validationError}>{validationError}</p>}

      <button
        type="button"
        className={styles.analyzeButton}
        onClick={handleAnalyzeClick}
        disabled={isLoading}
      >
        {isLoading ? "Analiz ediliyor..." : "Analiz Et"}
      </button>
    </div>
  );
}
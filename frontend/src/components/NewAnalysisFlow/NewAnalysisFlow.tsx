import { useState } from "react";
import styles from "./NewAnalysisFlow.module.css";
import FileUpload from "../FileUpload/FileUpload";
import ErrorAlert from "../ErrorAlert/ErrorAlert";
import LoadingIndicator from "../LoadingIndicator/LoadingIndicator";
import { createAnalysisJob } from "../../services/analysisJobApi";
import { LogAnalysisApiError } from "../../services/logAnalysisApi";

interface NewAnalysisFlowProps {
  onJobCreated: (jobId: string) => void;
}

function validateAnalysisName(name: string): string | null {
  const trimmed = name.trim();
  if (trimmed.length === 0) {
    return "Analiz adı zorunludur";
  }
  if (trimmed.length < 3) {
    return "Analiz adı en az 3 karakter olmalıdır";
  }
  if (trimmed.length > 100) {
    return "Analiz adı en fazla 100 karakter olabilir";
  }
  return null;
}

export default function NewAnalysisFlow({ onJobCreated }: NewAnalysisFlowProps) {
  const [analysisName, setAnalysisName] = useState("");
  const [nameError, setNameError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(file: File) {
    const validationError = validateAnalysisName(analysisName);
    if (validationError) {
      setNameError(validationError);
      return;
    }
    setNameError(null);
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
      const response = await createAnalysisJob(file, analysisName.trim());
      onJobCreated(response.jobId);
    } catch (error) {
      if (error instanceof LogAnalysisApiError) {
        setErrorMessage(error.message);
      } else {
        setErrorMessage("Backend servisine ulaşılamadı. Lütfen daha sonra tekrar deneyin.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.nameField}>
        <label htmlFor="analysis-name" className={styles.label}>
          Analiz Adı
        </label>
        <input
          id="analysis-name"
          type="text"
          className={styles.input}
          value={analysisName}
          onChange={(event) => {
            setAnalysisName(event.target.value);
            setNameError(null);
          }}
          placeholder="Örn. Ödeme Servisi Gece Logları"
          disabled={isSubmitting}
          data-testid="analysis-name-input"
        />
        {nameError && <p className={styles.validationError}>{nameError}</p>}
      </div>

      <FileUpload onAnalyze={handleSubmit} isLoading={isSubmitting} />

      {isSubmitting && <LoadingIndicator />}
      {errorMessage && <ErrorAlert message={errorMessage} />}
    </div>
  );
}
import { useState } from "react";
import styles from "./App.module.css";
import Header from "./components/Header/Header";
import FileUpload from "./components/FileUpload/FileUpload";
import LoadingIndicator from "./components/LoadingIndicator/LoadingIndicator";
import ErrorAlert from "./components/ErrorAlert/ErrorAlert";
import AnalysisSummary from "./components/AnalysisSummary/AnalysisSummary";
import FrequentErrorsTable from "./components/FrequentErrorsTable/FrequentErrorsTable";
import { analyzeLogFile, LogAnalysisApiError } from "./services/logAnalysisApi";
import type { LogAnalysisResponse } from "./types/logAnalysis";

export default function App() {
  const [result, setResult] = useState<LogAnalysisResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleAnalyze(file: File) {
    setIsLoading(true);
    setErrorMessage(null);
    setResult(null);

    try {
      const analysisResult = await analyzeLogFile(file);
      setResult(analysisResult);
    } catch (error) {
      if (error instanceof LogAnalysisApiError) {
        setErrorMessage(error.message);
      } else {
        setErrorMessage("Backend servisine ulaşılamadı. Lütfen daha sonra tekrar deneyin.");
      }
    } finally {
      setIsLoading(false);
    }
  }

  function handleReset() {
    setResult(null);
    setErrorMessage(null);
  }

  return (
    <div className={styles.app}>
      <Header />
      <main className={styles.main}>
        {!result && (
          <FileUpload onAnalyze={handleAnalyze} isLoading={isLoading} />
        )}

        {isLoading && <LoadingIndicator />}

        {errorMessage && <ErrorAlert message={errorMessage} />}

        {result && (
          <div className={styles.resultContainer}>
            <AnalysisSummary result={result} />
            <section>
              <h3 className={styles.sectionTitle}>En Sık Hatalar</h3>
              <FrequentErrorsTable errors={result.mostFrequentErrors} />
            </section>
            <button type="button" className={styles.newAnalysisButton} onClick={handleReset}>
              Yeni Analiz
            </button>
          </div>
        )}
      </main>
    </div>
  );
}
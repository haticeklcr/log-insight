import { useEffect, useState } from "react";
import styles from "./App.module.css";
import Header from "./components/Header/Header";
import NavigationTabs from "./components/NavigationTabs/NavigationTabs";
import type { ViewMode } from "./components/NavigationTabs/NavigationTabs";
import FileUpload from "./components/FileUpload/FileUpload";
import LoadingIndicator from "./components/LoadingIndicator/LoadingIndicator";
import ErrorAlert from "./components/ErrorAlert/ErrorAlert";
import AnalysisSummary from "./components/AnalysisSummary/AnalysisSummary";
import FrequentErrorsTable from "./components/FrequentErrorsTable/FrequentErrorsTable";
import HistoryView from "./components/HistoryView/HistoryView";
import AnalysisDetailView from "./components/AnalysisDetailView/AnalysisDetailView";
import { analyzeLogFile, fetchAnalysisDetail, LogAnalysisApiError } from "./services/logAnalysisApi";
import type { LogAnalysisResponse } from "./types/logAnalysis";
import type { AnalysisDetail } from "./types/analysisHistory";

export default function App() {
  const [view, setView] = useState<ViewMode>("new");

  const [result, setResult] = useState<LogAnalysisResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const [detailId, setDetailId] = useState<number | null>(null);
  const [detail, setDetail] = useState<AnalysisDetail | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

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

  useEffect(() => {
    if (detailId === null) {
      setDetail(null);
      return;
    }

    setIsDetailLoading(true);
    setDetailError(null);

    fetchAnalysisDetail(detailId)
      .then(setDetail)
      .catch(() => setDetailError("Analiz detayı yüklenirken bir hata oluştu"))
      .finally(() => setIsDetailLoading(false));
  }, [detailId]);

  function handleChangeView(nextView: ViewMode) {
    setView(nextView);
    setDetailId(null);
  }

  return (
    <div className={styles.app}>
      <Header />
      <NavigationTabs activeView={view} onChange={handleChangeView} />
      <main className={styles.main}>
        {view === "new" && (
          <>
            {!result && <FileUpload onAnalyze={handleAnalyze} isLoading={isLoading} />}
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
          </>
        )}

        {view === "history" && detailId === null && <HistoryView onViewDetail={setDetailId} />}

        {view === "history" && detailId !== null && (
          <>
            {isDetailLoading && <LoadingIndicator />}
            {detailError && <ErrorAlert message={detailError} />}
            {detail && <AnalysisDetailView detail={detail} onBack={() => setDetailId(null)} />}
          </>
        )}
      </main>
    </div>
  );
}
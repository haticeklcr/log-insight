import { useEffect, useState } from "react";
import styles from "./App.module.css";
import Header from "./components/Header/Header";
import NavigationTabs from "./components/NavigationTabs/NavigationTabs";
import type { ViewMode } from "./components/NavigationTabs/NavigationTabs";
import NewAnalysisFlow from "./components/NewAnalysisFlow/NewAnalysisFlow";
import LoadingIndicator from "./components/LoadingIndicator/LoadingIndicator";
import ErrorAlert from "./components/ErrorAlert/ErrorAlert";
import HistoryView from "./components/HistoryView/HistoryView";
import AnalysisDetailView from "./components/AnalysisDetailView/AnalysisDetailView";
import JobsListView from "./components/JobsListView/JobsListView";
import JobDetailView from "./components/JobDetailView/JobDetailView";
import { fetchAnalysisDetail } from "./services/logAnalysisApi";
import { cancelAnalysisJob, retryAnalysisJob } from "./services/analysisJobApi";
import { useJobPolling } from "./hooks/useJobPolling";
import type { AnalysisDetail } from "./types/analysisHistory";

export default function App() {
  const [view, setView] = useState<ViewMode>("new");

  const [detailId, setDetailId] = useState<number | null>(null);
  const [detail, setDetail] = useState<AnalysisDetail | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  const [activeJobId, setActiveJobId] = useState<string | null>(null);
  const { job: activeJob, errorMessage: pollingErrorMessage, refetch: refetchJob } = useJobPolling(activeJobId);

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
    setActiveJobId(null);
  }

  function handleJobCreated(jobId: string) {
    setView("jobs");
    setActiveJobId(jobId);
  }

  async function handleCancelJob() {
    if (!activeJobId) return;
    try {
      await cancelAnalysisJob(activeJobId);
    } finally {
      await refetchJob();
    }
  }

  async function handleRetryJob() {
    if (!activeJobId) return;
    try {
      await retryAnalysisJob(activeJobId);
    } finally {
      await refetchJob();
    }
  }

  function handleViewResultFromJob(analysisId: number) {
    setActiveJobId(null);
    setView("history");
    setDetailId(analysisId);
  }

  return (
    <div className={styles.app}>
      <Header />
      <NavigationTabs activeView={view} onChange={handleChangeView} />
      <main className={styles.main}>
        {view === "new" && <NewAnalysisFlow onJobCreated={handleJobCreated} />}

        {view === "jobs" && activeJobId === null && <JobsListView onViewDetail={setActiveJobId} />}

        {view === "jobs" && activeJobId !== null && (
          <>
            {!activeJob && !pollingErrorMessage && <LoadingIndicator />}
            {pollingErrorMessage && !activeJob && <ErrorAlert message={pollingErrorMessage} />}
            {activeJob && (
              <JobDetailView
                job={activeJob}
                pollingErrorMessage={pollingErrorMessage}
                onCancel={handleCancelJob}
                onRetry={handleRetryJob}
                onViewResult={handleViewResultFromJob}
                onBack={() => setActiveJobId(null)}
              />
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
import { useEffect, useState, useCallback } from "react";
import styles from "./HistoryView.module.css";
import HistoryTable from "../HistoryTable/HistoryTable";
import Pagination from "../Pagination/Pagination";
import SearchFilterBar from "../SearchFilterBar/SearchFilterBar";
import DeleteConfirmDialog from "../DeleteConfirmDialog/DeleteConfirmDialog";
import LoadingIndicator from "../LoadingIndicator/LoadingIndicator";
import ErrorAlert from "../ErrorAlert/ErrorAlert";
import { fetchAnalysisHistory, deleteAnalysis } from "../../services/logAnalysisApi";
import type { AnalysisSummary, PagedResponse } from "../../types/analysisHistory";

interface HistoryViewProps {
  onViewDetail: (id: number) => void;
}

const PAGE_SIZE = 10;

export default function HistoryView({ onViewDetail }: HistoryViewProps) {
  const [page, setPage] = useState(0);
  const [fileNameFilter, setFileNameFilter] = useState("");
  const [minErrorCountFilter, setMinErrorCountFilter] = useState<number | undefined>(undefined);
  const [data, setData] = useState<PagedResponse<AnalysisSummary> | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<AnalysisSummary | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const response = await fetchAnalysisHistory({
        page,
        size: PAGE_SIZE,
        fileName: fileNameFilter || undefined,
        minErrorCount: minErrorCountFilter,
      });
      setData(response);
    } catch {
      setErrorMessage("Analiz geçmişi yüklenirken bir hata oluştu. Backend servisine ulaşılamıyor olabilir.");
    } finally {
      setIsLoading(false);
    }
  }, [page, fileNameFilter, minErrorCountFilter]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  function handleApplyFilters(fileName: string, minErrorCount: string) {
    setFileNameFilter(fileName);
    setMinErrorCountFilter(minErrorCount ? Number(minErrorCount) : undefined);
    setPage(0);
  }

  async function handleConfirmDelete() {
    if (!pendingDelete) {
      return;
    }
    try {
      await deleteAnalysis(pendingDelete.id);
      setPendingDelete(null);
      setSuccessMessage("Analiz kaydı başarıyla silindi");
      await loadData();
    } catch {
      setErrorMessage("Analiz kaydı silinirken bir hata oluştu");
      setPendingDelete(null);
    }
  }

  return (
    <div className={styles.container}>
      <SearchFilterBar onApply={handleApplyFilters} />

      {successMessage && <p className={styles.success}>{successMessage}</p>}
      {isLoading && <LoadingIndicator />}
      {errorMessage && <ErrorAlert message={errorMessage} />}

      {!isLoading && !errorMessage && data && data.content.length === 0 && (
        <p className={styles.empty}>Henüz analiz geçmişi bulunmuyor.</p>
      )}

      {!isLoading && !errorMessage && data && data.content.length > 0 && (
        <>
          <HistoryTable
            analyses={data.content}
            onViewDetail={onViewDetail}
            onDelete={(analysis) => setPendingDelete(analysis)}
          />
          <Pagination
            page={data.page}
            totalPages={data.totalPages}
            first={data.first}
            last={data.last}
            onPageChange={setPage}
          />
        </>
      )}

      {pendingDelete && (
        <DeleteConfirmDialog
          fileName={pendingDelete.fileName}
          onConfirm={handleConfirmDelete}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </div>
  );
}
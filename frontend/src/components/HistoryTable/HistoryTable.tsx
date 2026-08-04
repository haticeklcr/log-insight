import { useTranslation } from "react-i18next";
import styles from "./HistoryTable.module.css";
import type { AnalysisSummary } from "../../types/analysisHistory";
import { formatFileSize, formatDateTime, formatDuration } from "../../utils/format";

interface HistoryTableProps {
  analyses: AnalysisSummary[];
  onViewDetail: (id: number) => void;
  onDelete: (analysis: AnalysisSummary) => void;
  analysisNameFilter: string;
  fileNameFilter: string;
  minErrorCountFilter: string;
  onAnalysisNameFilterChange: (value: string) => void;
  onFileNameFilterChange: (value: string) => void;
  onMinErrorCountFilterChange: (value: string) => void;
  onApplyFilters: () => void;
}

export default function HistoryTable({
  analyses,
  onViewDetail,
  onDelete,
  analysisNameFilter,
  fileNameFilter,
  minErrorCountFilter,
  onAnalysisNameFilterChange,
  onFileNameFilterChange,
  onMinErrorCountFilterChange,
  onApplyFilters,
}: HistoryTableProps) {
  const { t } = useTranslation();

  function handleEnterKey(event: React.KeyboardEvent) {
    if (event.key === "Enter") {
      onApplyFilters();
    }
  }

  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <th className={styles.headerCell}>{t("historyTable.analysisName")}</th>
          <th className={styles.headerCell}>{t("historyTable.fileName")}</th>
          <th className={styles.headerCell}>{t("historyTable.analyzedAt")}</th>
          <th className={styles.headerCell}>{t("historyTable.fileSize")}</th>
          <th className={styles.headerCell}>{t("historyTable.totalLines")}</th>
          <th className={styles.headerCell}>{t("historyTable.error")}</th>
          <th className={styles.headerCell}>{t("historyTable.exception")}</th>
          <th className={styles.headerCell}>{t("historyTable.duration")}</th>
          <th className={styles.headerCell}></th>
        </tr>
        <tr className={styles.filterRow}>
          <th className={styles.filterCell}>
            <input
              type="text"
              placeholder={t("searchFilterBar.analysisNamePlaceholder")}
              value={analysisNameFilter}
              onChange={(e) => onAnalysisNameFilterChange(e.target.value)}
              onKeyDown={handleEnterKey}
              className={styles.filterInput}
            />
          </th>
          <th className={styles.filterCell}>
            <input
              type="text"
              placeholder={t("searchFilterBar.fileNamePlaceholder")}
              value={fileNameFilter}
              onChange={(e) => onFileNameFilterChange(e.target.value)}
              onKeyDown={handleEnterKey}
              className={styles.filterInput}
            />
          </th>
          <th className={styles.filterCell} colSpan={3} />
          <th className={styles.filterCell}>
            <input
              type="number"
              min={0}
              placeholder={t("searchFilterBar.minErrorPlaceholder")}
              value={minErrorCountFilter}
              onChange={(e) => onMinErrorCountFilterChange(e.target.value)}
              onKeyDown={handleEnterKey}
              className={styles.filterInput}
            />
          </th>
          <th className={styles.filterCell} colSpan={2} />
          <th className={styles.filterCell}>
            <button type="button" className={styles.applyButton} onClick={onApplyFilters}>
              {t("searchFilterBar.apply")}
            </button>
          </th>
        </tr>
      </thead>
      <tbody>
        {analyses.map((analysis) => (
          <tr key={analysis.id}>
            <td className={styles.cell}>{analysis.analysisName ?? "—"}</td>
            <td className={styles.cell}>{analysis.fileName}</td>
            <td className={styles.cell}>{formatDateTime(analysis.analyzedAt)}</td>
            <td className={styles.cell}>{formatFileSize(analysis.fileSize)}</td>
            <td className={styles.cell}>{analysis.totalLines}</td>
            <td className={styles.cell}>{analysis.errorCount}</td>
            <td className={styles.cell}>{analysis.exceptionCount}</td>
            <td className={styles.cell}>{formatDuration(analysis.processingDurationMs)}</td>
            <td className={styles.cell}>
              <button type="button" className={styles.detailButton} onClick={() => onViewDetail(analysis.id)}>
                {t("historyTable.detail")}
              </button>
              <button type="button" className={styles.deleteButton} onClick={() => onDelete(analysis)}>
                {t("historyTable.delete")}
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
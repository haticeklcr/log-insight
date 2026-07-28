import { useTranslation } from "react-i18next";
import styles from "./HistoryTable.module.css";
import type { AnalysisSummary } from "../../types/analysisHistory";
import { formatFileSize, formatDateTime, formatDuration } from "../../utils/format";

interface HistoryTableProps {
  analyses: AnalysisSummary[];
  onViewDetail: (id: number) => void;
  onDelete: (analysis: AnalysisSummary) => void;
}

export default function HistoryTable({ analyses, onViewDetail, onDelete }: HistoryTableProps) {
  const { t } = useTranslation();
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
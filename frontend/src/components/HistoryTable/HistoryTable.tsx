import styles from "./HistoryTable.module.css";
import type { AnalysisSummary } from "../../types/analysisHistory";
import { formatFileSize, formatDateTime, formatDuration } from "../../utils/format";

interface HistoryTableProps {
  analyses: AnalysisSummary[];
  onViewDetail: (id: number) => void;
  onDelete: (analysis: AnalysisSummary) => void;
}

export default function HistoryTable({ analyses, onViewDetail, onDelete }: HistoryTableProps) {
  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <th className={styles.headerCell}>Dosya Adı</th>
          <th className={styles.headerCell}>Analiz Tarihi</th>
          <th className={styles.headerCell}>Boyut</th>
          <th className={styles.headerCell}>Toplam Satır</th>
          <th className={styles.headerCell}>ERROR</th>
          <th className={styles.headerCell}>Exception</th>
          <th className={styles.headerCell}>Süre</th>
          <th className={styles.headerCell}></th>
        </tr>
      </thead>
      <tbody>
        {analyses.map((analysis) => (
          <tr key={analysis.id}>
            <td className={styles.cell}>{analysis.fileName}</td>
            <td className={styles.cell}>{formatDateTime(analysis.analyzedAt)}</td>
            <td className={styles.cell}>{formatFileSize(analysis.fileSize)}</td>
            <td className={styles.cell}>{analysis.totalLines}</td>
            <td className={styles.cell}>{analysis.errorCount}</td>
            <td className={styles.cell}>{analysis.exceptionCount}</td>
            <td className={styles.cell}>{formatDuration(analysis.processingDurationMs)}</td>
            <td className={styles.cell}>
              <button type="button" className={styles.detailButton} onClick={() => onViewDetail(analysis.id)}>
                Detay
              </button>
              <button type="button" className={styles.deleteButton} onClick={() => onDelete(analysis)}>
                Sil
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
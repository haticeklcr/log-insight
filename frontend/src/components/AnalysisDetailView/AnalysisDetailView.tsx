import styles from "./AnalysisDetailView.module.css";
import StatCard from "../StatCard/StatCard";
import FrequentErrorsTable from "../FrequentErrorsTable/FrequentErrorsTable";
import type { AnalysisDetail } from "../../types/analysisHistory";
import { formatFileSize, formatDateTime, formatDuration } from "../../utils/format";

interface AnalysisDetailViewProps {
  detail: AnalysisDetail;
  onBack: () => void;
}

export default function AnalysisDetailView({ detail, onBack }: AnalysisDetailViewProps) {
  return (
    <div className={styles.container}>
      <button type="button" className={styles.backButton} onClick={onBack}>
        Geçmişe Dön
      </button>
      <h2 className={styles.title}>{detail.fileName}</h2>
      <div className={styles.meta}>
        <span>{formatFileSize(detail.fileSize)}</span>
        <span>{formatDateTime(detail.analyzedAt)}</span>
        <span>{formatDuration(detail.processingDurationMs)}</span>
      </div>
      <div className={styles.cards}>
        <StatCard label="Toplam Satır" value={detail.totalLines} />
        <StatCard label="INFO" value={detail.infoCount} />
        <StatCard label="WARN" value={detail.warningCount} />
        <StatCard label="ERROR" value={detail.errorCount} />
        <StatCard label="Exception" value={detail.exceptionCount} />
      </div>
      <section>
        <h3 className={styles.sectionTitle}>En Sık Hatalar</h3>
        <FrequentErrorsTable errors={detail.mostFrequentErrors} />
      </section>
    </div>
  );
}
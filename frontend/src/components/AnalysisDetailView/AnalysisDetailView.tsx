import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();
  return (
    <div className={styles.container}>
      <button type="button" className={styles.backButton} onClick={onBack}>
        {t("analysisDetail.back")}
      </button>
      <h2 className={styles.title}>{detail.fileName}</h2>
      <div className={styles.meta}>
        <span>{formatFileSize(detail.fileSize)}</span>
        <span>{formatDateTime(detail.analyzedAt)}</span>
        <span>{formatDuration(detail.processingDurationMs)}</span>
      </div>
      <div className={styles.cards}>
        <StatCard label={t("common.totalLines")} value={detail.totalLines} />
        <StatCard label={t("common.info")} value={detail.infoCount} />
        <StatCard label={t("common.warning")} value={detail.warningCount} />
        <StatCard label={t("common.error")} value={detail.errorCount} />
        <StatCard label={t("common.exception")} value={detail.exceptionCount} />
      </div>
      <section>
        <h3 className={styles.sectionTitle}>{t("analysisDetail.mostFrequentErrors")}</h3>
        <FrequentErrorsTable errors={detail.mostFrequentErrors} />
      </section>
    </div>
  );
}
import { useTranslation } from "react-i18next";
import styles from "./AnalysisDetailView.module.css";
import StatCard from "../StatCard/StatCard";
import FrequentErrorsTable from "../FrequentErrorsTable/FrequentErrorsTable";
import NamedCountTable from "../NamedCountTable/NamedCountTable";
import LogTimelineChart from "../LogTimelineChart/LogTimelineChart";
import type { AnalysisDetail } from "../../types/analysisHistory";
import { formatFileSize, formatDateTime, formatDuration } from "../../utils/format";

interface AnalysisDetailViewProps {
  detail: AnalysisDetail;
  onBack: () => void;
}

export default function AnalysisDetailView({ detail, onBack }: AnalysisDetailViewProps) {
  const { t } = useTranslation();

  const showFormatInfo = Boolean(detail.detectedLogFormat);
  const showHttpStats = (detail.statusCodeDistribution?.length ?? 0) > 0
    || (detail.httpMethodDistribution?.length ?? 0) > 0;
  const hasMaskableErrors = detail.mostFrequentErrors.length > 0;

  return (
    <div className={styles.container}>
      <button type="button" className={styles.backButton} onClick={onBack}>
        {t("analysisDetail.back")}
      </button>
      <h2 className={styles.title}>{detail.analysisName ?? detail.fileName}</h2>
      <p className={styles.fileNameSubtitle}>{detail.fileName}</p>
      <div className={styles.meta}>
        <span>{formatFileSize(detail.fileSize)}</span>
        <span>{formatDateTime(detail.analyzedAt)}</span>
        <span>{formatDuration(detail.processingDurationMs)}</span>
      </div>

      <div className={styles.cards}>
        <StatCard label={t("common.totalLines")} value={detail.totalLines} />
        <StatCard label={t("common.info")} value={detail.infoCount} variant="info" />
        <StatCard label={t("common.warning")} value={detail.warningCount} variant="warning" />
        <StatCard label={t("common.error")} value={detail.errorCount} variant="error" />
        <StatCard label={t("common.exception")} value={detail.exceptionCount} variant="exception" />
      </div>

      {showFormatInfo && (
        <section>
          <h3 className={styles.sectionTitle}>{t("analysisDetail.formatInfo")}</h3>
          <div className={styles.cards}>
            <StatCard
              label={t("analysisDetail.requestedParser")}
              value={detail.requestedParserType ?? t("analysisDetail.autoDetect")}
            />
            <StatCard label={t("analysisDetail.detectedFormat")} value={detail.detectedLogFormat ?? ""} />
            {detail.parseQualityScore != null && (
              <StatCard label={t("analysisDetail.parseQualityScore")} value={detail.parseQualityScore} />
            )}
            {detail.formatConfidence != null && (
              <StatCard label={t("analysisDetail.formatConfidence")} value={detail.formatConfidence} />
            )}
            {detail.parsedEntryCount != null && (
              <StatCard label={t("analysisDetail.parsedEntryCount")} value={detail.parsedEntryCount} />
            )}
            {detail.unparsedLineCount != null && (
              <StatCard label={t("analysisDetail.unparsedLineCount")} value={detail.unparsedLineCount} />
            )}
            {detail.unparsedLinePercentage != null && (
              <StatCard
                label={t("analysisDetail.unparsedLinePercentage")}
                value={`%${detail.unparsedLinePercentage.toFixed(1)}`}
              />
            )}
            {detail.multilineExceptionCount != null && (
              <StatCard label={t("analysisDetail.multilineExceptionCount")} value={detail.multilineExceptionCount} />
            )}
            {detail.firstLogTimestamp != null && (
              <StatCard label={t("analysisDetail.firstLogTimestamp")} value={formatDateTime(detail.firstLogTimestamp)} />
            )}
            {detail.lastLogTimestamp != null && (
              <StatCard label={t("analysisDetail.lastLogTimestamp")} value={formatDateTime(detail.lastLogTimestamp)} />
            )}
          </div>
        </section>
      )}

      <section>
        <h3 className={styles.sectionTitle}>{t("analysisDetail.mostFrequentErrors")}</h3>
        {hasMaskableErrors && <p className={styles.maskingNote}>{t("analysisDetail.sensitiveDataMasked")}</p>}
        <FrequentErrorsTable errors={detail.mostFrequentErrors} />
      </section>

      <section>
        <h3 className={styles.sectionTitle}>{t("analysisDetail.timeline")}</h3>
        <LogTimelineChart buckets={detail.timeline ?? []} />
      </section>

      <section>
        <h3 className={styles.sectionTitle}>{t("analysisDetail.mostFrequentLoggers")}</h3>
        <NamedCountTable
          items={(detail.mostFrequentLoggers ?? []).map((l) => ({ label: l.loggerName, count: l.count }))}
          labelHeader={t("analysisDetail.logger")}
        />
      </section>

      <section>
        <h3 className={styles.sectionTitle}>{t("analysisDetail.mostFrequentThreads")}</h3>
        <NamedCountTable
          items={(detail.mostFrequentThreads ?? []).map((th) => ({ label: th.threadName, count: th.count }))}
          labelHeader={t("analysisDetail.thread")}
        />
      </section>

      {showHttpStats && (
        <>
          <section>
            <h3 className={styles.sectionTitle}>{t("analysisDetail.statusCodeDistribution")}</h3>
            <NamedCountTable
              items={(detail.statusCodeDistribution ?? []).map((s) => ({ label: String(s.statusCode), count: s.count }))}
              labelHeader={t("analysisDetail.statusCode")}
            />
          </section>

          <section>
            <h3 className={styles.sectionTitle}>{t("analysisDetail.httpMethodDistribution")}</h3>
            <NamedCountTable
              items={(detail.httpMethodDistribution ?? []).map((m) => ({ label: m.httpMethod, count: m.count }))}
              labelHeader={t("analysisDetail.httpMethod")}
            />
          </section>
        </>
      )}
    </div>
  );
}
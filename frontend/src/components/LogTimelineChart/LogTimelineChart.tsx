import { useTranslation } from "react-i18next";
import styles from "./LogTimelineChart.module.css";
import type { TimelineBucket } from "../../types/analysisHistory";
import { formatDateTime } from "../../utils/format";

interface LogTimelineChartProps {
  buckets: TimelineBucket[];
}

export default function LogTimelineChart({ buckets }: LogTimelineChartProps) {
  const { t } = useTranslation();

  if (buckets.length === 0) {
    return <p className={styles.empty}>{t("analysisDetail.noData")}</p>;
  }

  const maxCount = Math.max(...buckets.map((bucket) => bucket.totalCount), 1);

  return (
    <div className={styles.chart} data-testid="log-timeline-chart">
      {buckets.map((bucket) => {
        const heightPercent = Math.round((bucket.totalCount / maxCount) * 100);
        const tooltip = `${formatDateTime(bucket.bucketStart)} — ${t("common.totalLines")}: ${bucket.totalCount}, ${t("common.error")}: ${bucket.errorCount}`;
        return (
          <div key={bucket.bucketStart} className={styles.barColumn} title={tooltip}>
            <div className={styles.barTrack}>
              <div className={styles.barErrorPortion} style={{ height: `${Math.round((bucket.errorCount / maxCount) * 100)}%` }} />
              <div className={styles.barTotalPortion} style={{ height: `${heightPercent}%` }} />
            </div>
          </div>
        );
      })}
    </div>
  );
}
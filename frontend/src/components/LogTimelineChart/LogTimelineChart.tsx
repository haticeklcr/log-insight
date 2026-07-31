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
    <div>
      <div className={styles.legend}>
        <span className={styles.legendItem}>
          <span className={`${styles.legendDot} ${styles.infoColor}`} /> {t("common.info")}
        </span>
        <span className={styles.legendItem}>
          <span className={`${styles.legendDot} ${styles.warnColor}`} /> {t("common.warning")}
        </span>
        <span className={styles.legendItem}>
          <span className={`${styles.legendDot} ${styles.errorColor}`} /> {t("common.error")}
        </span>
      </div>
      <div className={styles.chart} data-testid="log-timeline-chart">
        {buckets.map((bucket) => {
          const tooltip = `${formatDateTime(bucket.bucketStart)} — ${t("common.totalLines")}: ${bucket.totalCount}, `
            + `${t("common.info")}: ${bucket.infoCount}, ${t("common.warning")}: ${bucket.warnCount}, `
            + `${t("common.error")}: ${bucket.errorCount}`;
          const infoHeight = Math.round((bucket.infoCount / maxCount) * 100);
          const warnHeight = Math.round((bucket.warnCount / maxCount) * 100);
          const errorHeight = Math.round((bucket.errorCount / maxCount) * 100);
          return (
            <div key={bucket.bucketStart} className={styles.barColumn} title={tooltip}>
              <div className={styles.barTrack}>
                <div className={styles.errorSegment} style={{ height: `${errorHeight}%` }} />
                <div className={styles.warnSegment} style={{ height: `${warnHeight}%` }} />
                <div className={styles.infoSegment} style={{ height: `${infoHeight}%` }} />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
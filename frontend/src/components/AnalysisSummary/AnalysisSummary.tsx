import styles from "./AnalysisSummary.module.css";
import StatCard from "../StatCard/StatCard";
import type { LogAnalysisResponse } from "../../types/logAnalysis";

interface AnalysisSummaryProps {
  result: LogAnalysisResponse;
}

export default function AnalysisSummary({ result }: AnalysisSummaryProps) {
  return (
    <section className={styles.container}>
      <h2 className={styles.title}>{result.fileName}</h2>
      <div className={styles.cards}>
        <StatCard label="Toplam Satır" value={result.totalLines} />
        <StatCard label="INFO" value={result.infoCount} variant="info" />
        <StatCard label="WARN" value={result.warningCount} variant="warning" />
        <StatCard label="ERROR" value={result.errorCount} variant="error" />
        <StatCard label="Exception" value={result.exceptionCount} variant="exception" />
      </div>
    </section>
  );
}
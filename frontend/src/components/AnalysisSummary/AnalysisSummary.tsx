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
        <StatCard label="INFO" value={result.infoCount} />
        <StatCard label="WARN" value={result.warningCount} />
        <StatCard label="ERROR" value={result.errorCount} />
        <StatCard label="Exception" value={result.exceptionCount} />
      </div>
    </section>
  );
}
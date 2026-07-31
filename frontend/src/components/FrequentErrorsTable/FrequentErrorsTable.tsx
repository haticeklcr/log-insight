import { useTranslation } from "react-i18next";
import styles from "./FrequentErrorsTable.module.css";
import type { ErrorFrequency } from "../../types/logAnalysis";

interface FrequentErrorsTableProps {
  errors: ErrorFrequency[];
}

export default function FrequentErrorsTable({ errors }: FrequentErrorsTableProps) {
  const { t } = useTranslation();

  if (errors.length === 0) {
    return <p className={styles.empty}>{t("frequentErrorsTable.empty")}</p>;
  }

  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <th className={styles.headerCell}>{t("frequentErrorsTable.normalizedMessage")}</th>
          <th className={styles.headerCell}>{t("frequentErrorsTable.count")}</th>
          <th className={styles.headerCell}>{t("frequentErrorsTable.sampleMessage")}</th>
        </tr>
      </thead>
      <tbody>
        {errors.map((error) => (
          <tr key={error.message}>
            <td className={styles.cell}>{error.normalizedMessage ?? error.message}</td>
            <td className={styles.cell}>{error.count}</td>
            <td className={styles.cell}>{error.message}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
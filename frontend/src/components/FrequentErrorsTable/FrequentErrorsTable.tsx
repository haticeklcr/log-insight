import styles from "./FrequentErrorsTable.module.css";
import type { ErrorFrequency } from "../../types/logAnalysis";

interface FrequentErrorsTableProps {
  errors: ErrorFrequency[];
}

export default function FrequentErrorsTable({ errors }: FrequentErrorsTableProps) {
  if (errors.length === 0) {
    return <p className={styles.empty}>Tekrar eden hata mesajı bulunamadı.</p>;
  }

  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <th className={styles.headerCell}>Hata Mesajı</th>
          <th className={styles.headerCell}>Tekrar Sayısı</th>
        </tr>
      </thead>
      <tbody>
        {errors.map((error) => (
          <tr key={error.message}>
            <td className={styles.cell}>{error.message}</td>
            <td className={styles.cell}>{error.count}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
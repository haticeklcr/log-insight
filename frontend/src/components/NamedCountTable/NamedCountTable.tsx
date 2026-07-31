import { useTranslation } from "react-i18next";
import styles from "./NamedCountTable.module.css";

export interface NamedCountItem {
  label: string;
  count: number;
}

interface NamedCountTableProps {
  items: NamedCountItem[];
  labelHeader: string;
}

export default function NamedCountTable({ items, labelHeader }: NamedCountTableProps) {
  const { t } = useTranslation();

  if (items.length === 0) {
    return <p className={styles.empty}>{t("analysisDetail.noData")}</p>;
  }

  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <th className={styles.headerCell}>{labelHeader}</th>
          <th className={styles.headerCell}>{t("analysisDetail.count")}</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr key={item.label}>
            <td className={styles.cell}>{item.label}</td>
            <td className={styles.cell}>{item.count}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
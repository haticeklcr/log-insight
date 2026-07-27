import { useTranslation } from "react-i18next";
import styles from "./Pagination.module.css";

interface PaginationProps {
  page: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  onPageChange: (page: number) => void;
}

export default function Pagination({ page, totalPages, first, last, onPageChange }: PaginationProps) {
  const { t } = useTranslation();
  return (
    <div className={styles.container}>
      <button type="button" onClick={() => onPageChange(page - 1)} disabled={first} className={styles.button}>
        {t("pagination.previous")}
      </button>
      <span className={styles.info}>
        {t("pagination.pageInfo", { current: page + 1, total: Math.max(totalPages, 1) })}
      </span>
      <button type="button" onClick={() => onPageChange(page + 1)} disabled={last} className={styles.button}>
        {t("pagination.next")}
      </button>
    </div>
  );
}
import styles from "./Pagination.module.css";

interface PaginationProps {
  page: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  onPageChange: (page: number) => void;
}

export default function Pagination({ page, totalPages, first, last, onPageChange }: PaginationProps) {
  return (
    <div className={styles.container}>
      <button type="button" onClick={() => onPageChange(page - 1)} disabled={first} className={styles.button}>
        Önceki Sayfa
      </button>
      <span className={styles.info}>
        Sayfa {page + 1} / {Math.max(totalPages, 1)}
      </span>
      <button type="button" onClick={() => onPageChange(page + 1)} disabled={last} className={styles.button}>
        Sonraki Sayfa
      </button>
    </div>
  );
}
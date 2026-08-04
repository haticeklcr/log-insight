import styles from "./StatCard.module.css";

type StatCardVariant = "default" | "info" | "warning" | "error" | "exception";

interface StatCardProps {
  label: string;
  value: number | string;
  variant?: StatCardVariant;
}

export default function StatCard({ label, value, variant = "default" }: StatCardProps) {
  const valueClassName = variant === "default" ? styles.value : `${styles.value} ${styles[variant]}`;

  return (
    <div className={styles.card}>
      <span className={valueClassName}>{value}</span>
      <span className={styles.label}>{label}</span>
    </div>
  );
}
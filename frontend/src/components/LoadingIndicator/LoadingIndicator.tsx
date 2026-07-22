import styles from "./LoadingIndicator.module.css";

export default function LoadingIndicator() {
  return (
    <div className={styles.container} role="status">
      <span className={styles.spinner} />
      <span className={styles.text}>Dosya analiz ediliyor...</span>
    </div>
  );
}
import styles from "./Header.module.css";
import BackendStatus from "../BackendStatus/BackendStatus";

export default function Header() {
  return (
    <header className={styles.header}>
      <div className={styles.titleGroup}>
        <h1 className={styles.title}>Log Insight</h1>
        <p className={styles.subtitle}>
          Uygulama loglarınızı yükleyin, hataları ve tekrar eden sorunları anında görün.
        </p>
      </div>
      <BackendStatus />
    </header>
  );
}
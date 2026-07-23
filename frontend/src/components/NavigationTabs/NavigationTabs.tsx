import styles from "./NavigationTabs.module.css";

export type ViewMode = "new" | "history";

interface NavigationTabsProps {
  activeView: ViewMode;
  onChange: (view: ViewMode) => void;
}

export default function NavigationTabs({ activeView, onChange }: NavigationTabsProps) {
  return (
    <nav className={styles.tabs}>
      <button
        type="button"
        className={`${styles.tab} ${activeView === "new" ? styles.tabActive : ""}`}
        onClick={() => onChange("new")}
      >
        Yeni Analiz
      </button>
      <button
        type="button"
        className={`${styles.tab} ${activeView === "history" ? styles.tabActive : ""}`}
        onClick={() => onChange("history")}
      >
        Analiz Geçmişi
      </button>
    </nav>
  );
}
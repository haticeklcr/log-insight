import { useTranslation } from "react-i18next";
import styles from "./NavigationTabs.module.css";

export type ViewMode = "new" | "jobs" | "history";

interface NavigationTabsProps {
  activeView: ViewMode;
  onChange: (view: ViewMode) => void;
}

export default function NavigationTabs({ activeView, onChange }: NavigationTabsProps) {
  const { t } = useTranslation();

  return (
    <nav className={styles.tabs}>
      <button
        type="button"
        className={`${styles.tab} ${activeView === "new" ? styles.tabActive : ""}`}
        onClick={() => onChange("new")}
      >
        {t("nav.newAnalysis")}
      </button>
      <button
        type="button"
        className={`${styles.tab} ${activeView === "jobs" ? styles.tabActive : ""}`}
        onClick={() => onChange("jobs")}
      >
        {t("nav.jobs")}
      </button>
      <button
        type="button"
        className={`${styles.tab} ${activeView === "history" ? styles.tabActive : ""}`}
        onClick={() => onChange("history")}
      >
        {t("nav.history")}
      </button>
    </nav>
  );
}
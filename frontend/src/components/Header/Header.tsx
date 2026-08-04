import { useTranslation } from "react-i18next";
import styles from "./Header.module.css";
import BackendStatus from "../BackendStatus/BackendStatus";
import LanguageSwitcher from "../LanguageSwitcher/LanguageSwitcher";
import ThemeSwitcher from "../ThemeSwitcher/ThemeSwitcher";

export default function Header() {
  const { t } = useTranslation();

  return (
    <header className={styles.header}>
      <div className={styles.titleGroup}>
        <h1 className={styles.title}>{t("header.title")}</h1>
        <p className={styles.subtitle}>{t("header.subtitle")}</p>
      </div>
      <div className={styles.rightGroup}>
        <BackendStatus />
        <span className={styles.divider} />
        <ThemeSwitcher />
        <LanguageSwitcher />
      </div>
    </header>
  );
}
import { useTranslation } from "react-i18next";
import styles from "./LanguageSwitcher.module.css";
import type { SupportedLanguage } from "../../i18n";

export default function LanguageSwitcher() {
  const { i18n, t } = useTranslation();

  function handleChange(language: SupportedLanguage) {
    i18n.changeLanguage(language);
  }

  return (
    <div className={styles.container} role="group" aria-label="Dil seçimi">
      <button
        type="button"
        className={`${styles.option} ${i18n.language === "tr" ? styles.active : ""}`}
        onClick={() => handleChange("tr")}
        title={t("language.turkish")}
      >
        TR
      </button>
      <button
        type="button"
        className={`${styles.option} ${i18n.language === "en" ? styles.active : ""}`}
        onClick={() => handleChange("en")}
        title={t("language.english")}
      >
        EN
      </button>
    </div>
  );
}
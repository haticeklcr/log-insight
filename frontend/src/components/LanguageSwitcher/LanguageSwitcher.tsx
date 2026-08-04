import { useTranslation } from "react-i18next";
import styles from "./LanguageSwitcher.module.css";
import type { SupportedLanguage } from "../../i18n";

export default function LanguageSwitcher() {
  const { i18n, t } = useTranslation();
  const current = i18n.language as SupportedLanguage;
  const target: SupportedLanguage = current === "tr" ? "en" : "tr";

  function handleClick() {
    i18n.changeLanguage(target);
  }

  return (
    <button
      type="button"
      className={styles.button}
      onClick={handleClick}
      title={target === "en" ? t("language.english") : t("language.turkish")}
    >
      {target === "en" ? "EN" : "TR"}
    </button>
  );
}
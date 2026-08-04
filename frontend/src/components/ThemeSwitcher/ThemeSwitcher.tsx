import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import styles from "./ThemeSwitcher.module.css";
import { applyTheme, getStoredTheme } from "../../theme";
import type { ThemeMode } from "../../theme";

export default function ThemeSwitcher() {
  const { t } = useTranslation();
  const [theme, setTheme] = useState<ThemeMode>(getStoredTheme());

  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  const target: ThemeMode = theme === "dark" ? "light" : "dark";

  function handleClick() {
    setTheme(target);
  }

  return (
    <button
      type="button"
      className={styles.button}
      onClick={handleClick}
      title={target === "light" ? t("theme.light") : t("theme.dark")}
    >
      {target === "light" ? t("theme.lightShort") : t("theme.darkShort")}
    </button>
  );
}
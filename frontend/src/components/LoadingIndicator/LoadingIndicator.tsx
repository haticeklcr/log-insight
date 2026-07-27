import { useTranslation } from "react-i18next";
import styles from "./LoadingIndicator.module.css";

export default function LoadingIndicator() {
  const { t } = useTranslation();
  return (
    <div className={styles.container} role="status">
      <span className={styles.spinner} />
      <span className={styles.text}>{t("common.loading")}</span>
    </div>
  );
}
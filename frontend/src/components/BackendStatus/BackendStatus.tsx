import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import styles from "./BackendStatus.module.css";
import { checkBackendHealth } from "../../services/logAnalysisApi";

type Status = "checking" | "up" | "down";

export default function BackendStatus() {
  const { t } = useTranslation();
  const [status, setStatus] = useState<Status>("checking");

  useEffect(() => {
    let isMounted = true;

    checkBackendHealth().then((isUp) => {
      if (isMounted) {
        setStatus(isUp ? "up" : "down");
      }
    });

    return () => {
      isMounted = false;
    };
  }, []);

  const label =
    status === "checking"
      ? t("backendStatus.checking")
      : status === "up"
        ? t("backendStatus.up")
        : t("backendStatus.down");

  return (
    <div className={styles.container} data-status={status}>
      <span className={styles.dot} />
      <span className={styles.label}>{label}</span>
    </div>
  );
}
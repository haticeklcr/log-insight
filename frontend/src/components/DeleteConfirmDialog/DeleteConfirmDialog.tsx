import { useTranslation } from "react-i18next";
import styles from "./DeleteConfirmDialog.module.css";

interface DeleteConfirmDialogProps {
  fileName: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export default function DeleteConfirmDialog({ fileName, onConfirm, onCancel }: DeleteConfirmDialogProps) {
  const { t } = useTranslation();
  return (
    <div className={styles.overlay}>
      <div className={styles.dialog}>
        <p className={styles.message}>{t("deleteConfirm.message", { fileName })}</p>
        <div className={styles.actions}>
          <button type="button" className={styles.cancelButton} onClick={onCancel}>
            {t("deleteConfirm.cancel")}
          </button>
          <button type="button" className={styles.confirmButton} onClick={onConfirm}>
            {t("deleteConfirm.confirm")}
          </button>
        </div>
      </div>
    </div>
  );
}
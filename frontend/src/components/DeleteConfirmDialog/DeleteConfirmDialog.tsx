import styles from "./DeleteConfirmDialog.module.css";

interface DeleteConfirmDialogProps {
  fileName: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export default function DeleteConfirmDialog({ fileName, onConfirm, onCancel }: DeleteConfirmDialogProps) {
  return (
    <div className={styles.overlay}>
      <div className={styles.dialog}>
        <p className={styles.message}>
          <strong>{fileName}</strong> adlı analiz kaydını silmek istediğinize emin misiniz?
        </p>
        <div className={styles.actions}>
          <button type="button" className={styles.cancelButton} onClick={onCancel}>
            Vazgeç
          </button>
          <button type="button" className={styles.confirmButton} onClick={onConfirm}>
            Sil
          </button>
        </div>
      </div>
    </div>
  );
}
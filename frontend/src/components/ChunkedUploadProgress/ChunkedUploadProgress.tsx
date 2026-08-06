import { useTranslation } from "react-i18next";
import styles from "./ChunkedUploadProgress.module.css";
import type { ChunkedUploadState } from "../../hooks/useChunkedUpload";

interface ChunkedUploadProgressProps {
  state: ChunkedUploadState;
  onCancel: () => void;
}

export default function ChunkedUploadProgress({ state, onCancel }: ChunkedUploadProgressProps) {
  const { t } = useTranslation();

  if (state.phase === "idle" || state.phase === "cancelled") {
    return null;
  }

  const uploadPercent =
    state.totalChunks > 0 ? Math.round((state.uploadedChunks / state.totalChunks) * 100) : 0;
  const showMergeRow = state.phase === "merging" || state.phase === "completed";
  const showCancelButton = state.phase === "uploading" || state.phase === "merging";

  return (
    <div className={styles.container} data-testid="chunked-upload-progress">
      <div>
        <div className={styles.phaseRow}>
          <span>{t("chunkedUpload.uploading")}</span>
          <span>
            {state.uploadedChunks}/{state.totalChunks}
          </span>
        </div>
        <div className={styles.progressTrack}>
          <div className={styles.progressFill} style={{ width: `${uploadPercent}%` }} />
        </div>
      </div>

      {showMergeRow && (
        <div>
          <div className={styles.phaseRow}>
            <span>{t("chunkedUpload.merging")}</span>
            <span>{state.mergeProgress}%</span>
          </div>
          <div className={styles.progressTrack}>
            <div className={styles.progressFill} style={{ width: `${state.mergeProgress}%` }} />
          </div>
        </div>
      )}

      {state.phase === "failed" && (
        <p className={styles.errorText}>
          {state.errorMessage === "mergeFailed"
            ? t("chunkedUpload.mergeFailed")
            : t("chunkedUpload.uploadFailed")}
        </p>
      )}

      {showCancelButton && (
        <button type="button" className={styles.cancelButton} onClick={onCancel}>
          {t("chunkedUpload.cancelButton")}
        </button>
      )}
    </div>
  );
}
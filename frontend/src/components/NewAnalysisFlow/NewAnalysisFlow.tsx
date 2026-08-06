import { useEffect, useRef, useState } from "react";
import type { ChangeEvent } from "react";
import { useTranslation } from "react-i18next";
import styles from "./NewAnalysisFlow.module.css";
import FileUpload from "../FileUpload/FileUpload";
import ChunkedUploadProgress from "../ChunkedUploadProgress/ChunkedUploadProgress";
import ErrorAlert from "../ErrorAlert/ErrorAlert";
import ParserAndFilterFields, {
  EMPTY_FILTER_STATE,
  type FilterFormState,
} from "../ParserAndFilterFields/ParserAndFilterFields";
import { createAnalysisJobFromUpload } from "../../services/analysisJobApi";
import { translateApiError } from "../../utils/apiErrorMessage";
import type { LogParserType } from "../../types/analysisJob";
import {
  clearPendingUpload,
  loadPendingUpload,
  useChunkedUpload,
  type PendingUploadInfo,
} from "../../hooks/useChunkedUpload";

interface NewAnalysisFlowProps {
  onJobCreated: (jobId: string) => void;
}

function toIsoOrUndefined(localDateTimeValue: string): string | undefined {
  if (!localDateTimeValue) return undefined;
  const date = new Date(localDateTimeValue);
  if (Number.isNaN(date.getTime())) return undefined;
  return date.toISOString();
}

export default function NewAnalysisFlow({ onJobCreated }: NewAnalysisFlowProps) {
  const { t } = useTranslation();
  const upload = useChunkedUpload();
  const resumeInputRef = useRef<HTMLInputElement>(null);

  function validateAnalysisName(name: string): string | null {
    const trimmed = name.trim();
    if (trimmed.length === 0) {
      return t("newAnalysis.nameRequired");
    }
    if (trimmed.length < 3) {
      return t("newAnalysis.nameTooShort");
    }
    if (trimmed.length > 100) {
      return t("newAnalysis.nameTooLong");
    }
    return null;
  }

  const [analysisName, setAnalysisName] = useState("");
  const [nameError, setNameError] = useState<string | null>(null);
  const [parserType, setParserType] = useState<LogParserType>("AUTO");
  const [filters, setFilters] = useState<FilterFormState>(EMPTY_FILTER_STATE);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [pendingUpload, setPendingUpload] = useState<PendingUploadInfo | null>(null);
  const [resumeMismatch, setResumeMismatch] = useState(false);

  useEffect(() => {
    setPendingUpload(loadPendingUpload());
  }, []);

  useEffect(() => {
    if (upload.state.phase === "completed" && upload.state.uploadId) {
      void finalizeJobCreation(upload.state.uploadId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [upload.state.phase]);

  function buildOptions() {
    const options = {
      parserType: parserType === "AUTO" ? undefined : parserType,
      startTime: toIsoOrUndefined(filters.startTime),
      endTime: toIsoOrUndefined(filters.endTime),
      levels: filters.levels.length > 0 ? filters.levels : undefined,
      logger: filters.logger.trim() || undefined,
      thread: filters.thread.trim() || undefined,
      messageContains: filters.messageContains.trim() || undefined,
      statusCodes: filters.statusCodes.trim()
        ? filters.statusCodes.split(",").map((v) => v.trim()).filter(Boolean)
        : undefined,
      httpMethods: filters.httpMethods.trim()
        ? filters.httpMethods.split(",").map((v) => v.trim()).filter(Boolean)
        : undefined,
      pathContains: filters.pathContains.trim() || undefined,
    };
    const hasOptions = Object.values(options).some((value) => value !== undefined);
    return hasOptions ? options : undefined;
  }

  async function finalizeJobCreation(uploadId: string) {
    setIsSubmitting(true);
    try {
      const options = buildOptions();
      const response = await createAnalysisJobFromUpload(uploadId, analysisName.trim(), options);
      upload.reset();
      onJobCreated(response.jobId);
    } catch (error) {
      setErrorMessage(translateApiError(error, t, "newAnalysis.backendUnreachable"));
      upload.reset();
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleSubmit(file: File) {
    const validationError = validateAnalysisName(analysisName);
    if (validationError) {
      setNameError(validationError);
      return;
    }
    setNameError(null);
    setErrorMessage(null);
    await upload.start(file);
  }

  async function handleResumeFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file || !pendingUpload) return;

    const matches =
      file.name === pendingUpload.fileName &&
      file.size === pendingUpload.fileSize &&
      file.lastModified === pendingUpload.fileLastModified;

    if (!matches) {
      setResumeMismatch(true);
      return;
    }

    setResumeMismatch(false);
    const infoToResume = pendingUpload;
    setPendingUpload(null);
    await upload.resume(file, infoToResume);
  }

  function handleDiscardPendingUpload() {
    clearPendingUpload();
    setPendingUpload(null);
    setResumeMismatch(false);
  }

  const isBusy = isSubmitting || upload.state.phase === "uploading" || upload.state.phase === "merging";

  return (
    <div className={styles.container}>
      <div className={styles.nameField}>
        <label htmlFor="analysis-name" className={styles.label}>
          {t("newAnalysis.nameLabel")}
        </label>
        <input
          id="analysis-name"
          type="text"
          className={styles.input}
          value={analysisName}
          onChange={(event) => {
            setAnalysisName(event.target.value);
            setNameError(null);
          }}
          placeholder={t("newAnalysis.namePlaceholder")}
          disabled={isBusy}
          data-testid="analysis-name-input"
        />
        {nameError && <p className={styles.validationError}>{nameError}</p>}
      </div>

      <ParserAndFilterFields
        parserType={parserType}
        onParserTypeChange={setParserType}
        filters={filters}
        onFiltersChange={setFilters}
        disabled={isBusy}
      />

      {pendingUpload && upload.state.phase === "idle" && (
        <div className={styles.resumeBanner} data-testid="resume-banner">
          <p className={styles.resumeText}>
            {t("chunkedUpload.resumeFound")}: {pendingUpload.fileName}
          </p>
          {resumeMismatch && <p className={styles.validationError}>{t("chunkedUpload.resumeMismatch")}</p>}
          <div className={styles.resumeActions}>
            <button
              type="button"
              className={styles.resumeButton}
              onClick={() => resumeInputRef.current?.click()}
            >
              {t("chunkedUpload.resumeButton")}
            </button>
            <button type="button" className={styles.discardButton} onClick={handleDiscardPendingUpload}>
              {t("chunkedUpload.discardButton")}
            </button>
          </div>
          <input
            ref={resumeInputRef}
            type="file"
            accept=".log,.txt"
            className={styles.hiddenInput}
            onChange={handleResumeFileSelected}
            data-testid="resume-file-input"
          />
        </div>
      )}

      {upload.state.phase === "idle" || upload.state.phase === "cancelled" ? (
        <FileUpload onAnalyze={handleSubmit} isLoading={isBusy} />
      ) : (
        <ChunkedUploadProgress state={upload.state} onCancel={() => void upload.cancel()} />
      )}

      {errorMessage && <ErrorAlert message={errorMessage} />}
    </div>
  );
}
import { useState } from "react";
import { useTranslation } from "react-i18next";
import styles from "./NewAnalysisFlow.module.css";
import FileUpload from "../FileUpload/FileUpload";
import ErrorAlert from "../ErrorAlert/ErrorAlert";
import LoadingIndicator from "../LoadingIndicator/LoadingIndicator";
import ParserAndFilterFields, {
  EMPTY_FILTER_STATE,
  type FilterFormState,
} from "../ParserAndFilterFields/ParserAndFilterFields";
import { createAnalysisJob } from "../../services/analysisJobApi";
import { translateApiError } from "../../utils/apiErrorMessage";
import type { LogParserType } from "../../types/analysisJob";

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

  async function handleSubmit(file: File) {
    const validationError = validateAnalysisName(analysisName);
    if (validationError) {
      setNameError(validationError);
      return;
    }
    setNameError(null);
    setErrorMessage(null);
    setIsSubmitting(true);

    try {
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

      const response = hasOptions
        ? await createAnalysisJob(file, analysisName.trim(), options)
        : await createAnalysisJob(file, analysisName.trim());
      onJobCreated(response.jobId);
    } catch (error) {
      setErrorMessage(translateApiError(error, t, "newAnalysis.backendUnreachable"));
    } finally {
      setIsSubmitting(false);
    }
  }

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
          disabled={isSubmitting}
          data-testid="analysis-name-input"
        />
        {nameError && <p className={styles.validationError}>{nameError}</p>}
      </div>

      <ParserAndFilterFields
        parserType={parserType}
        onParserTypeChange={setParserType}
        filters={filters}
        onFiltersChange={setFilters}
        disabled={isSubmitting}
      />

      <FileUpload onAnalyze={handleSubmit} isLoading={isSubmitting} />

      {isSubmitting && <LoadingIndicator />}
      {errorMessage && <ErrorAlert message={errorMessage} />}
    </div>
  );
}
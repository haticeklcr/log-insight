import { useState } from "react";
import { useTranslation } from "react-i18next";
import styles from "./ParserAndFilterFields.module.css";
import type { LogParserType } from "../../types/analysisJob";

export interface FilterFormState {
  startTime: string;
  endTime: string;
  levels: string[];
  logger: string;
  thread: string;
  messageContains: string;
  statusCodes: string;
  httpMethods: string;
  pathContains: string;
}

export const EMPTY_FILTER_STATE: FilterFormState = {
  startTime: "",
  endTime: "",
  levels: [],
  logger: "",
  thread: "",
  messageContains: "",
  statusCodes: "",
  httpMethods: "",
  pathContains: "",
};

const PARSER_OPTIONS: LogParserType[] = [
  "AUTO",
  "SPRING_BOOT",
  "JSON",
  "NGINX_ACCESS",
  "APACHE_ACCESS",
  "PLAIN_TEXT",
];

const LEVEL_OPTIONS = ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"];

const LOGGER_THREAD_FORMATS: LogParserType[] = ["AUTO", "SPRING_BOOT", "JSON"];
const HTTP_FORMATS: LogParserType[] = ["AUTO", "NGINX_ACCESS", "APACHE_ACCESS"];

interface ParserAndFilterFieldsProps {
  parserType: LogParserType;
  onParserTypeChange: (value: LogParserType) => void;
  filters: FilterFormState;
  onFiltersChange: (filters: FilterFormState) => void;
  disabled: boolean;
}

export default function ParserAndFilterFields({
  parserType,
  onParserTypeChange,
  filters,
  onFiltersChange,
  disabled,
}: ParserAndFilterFieldsProps) {
  const { t } = useTranslation();
  const [isExpanded, setIsExpanded] = useState(false);

  const showLoggerThread = LOGGER_THREAD_FORMATS.includes(parserType);
  const showHttpFields = HTTP_FORMATS.includes(parserType);

  function updateFilter<K extends keyof FilterFormState>(key: K, value: FilterFormState[K]) {
    onFiltersChange({ ...filters, [key]: value });
  }

  function toggleLevel(level: string) {
    const nextLevels = filters.levels.includes(level)
      ? filters.levels.filter((l) => l !== level)
      : [...filters.levels, level];
    updateFilter("levels", nextLevels);
  }

  return (
    <div className={styles.container}>
      <div className={styles.parserField}>
        <label htmlFor="parser-type-select" className={styles.label}>
          {t("newAnalysis.parser.label")}
        </label>
        <select
          id="parser-type-select"
          className={styles.select}
          value={parserType}
          disabled={disabled}
          onChange={(event) => onParserTypeChange(event.target.value as LogParserType)}
          data-testid="parser-type-select"
        >
          {PARSER_OPTIONS.map((option) => (
            <option key={option} value={option}>
              {t(`newAnalysis.parser.options.${option}`)}
            </option>
          ))}
        </select>
      </div>

      <button
        type="button"
        className={styles.toggleButton}
        onClick={() => setIsExpanded((prev) => !prev)}
        disabled={disabled}
        data-testid="toggle-advanced-filters"
      >
        {isExpanded ? t("newAnalysis.filters.hide") : t("newAnalysis.filters.show")}
      </button>

      {isExpanded && (
        <div className={styles.filterGrid} data-testid="advanced-filters-panel">
          <div className={styles.field}>
            <label className={styles.label}>{t("newAnalysis.filters.startTime")}</label>
            <input
              type="datetime-local"
              className={styles.input}
              value={filters.startTime}
              disabled={disabled}
              onChange={(event) => updateFilter("startTime", event.target.value)}
            />
          </div>

          <div className={styles.field}>
            <label className={styles.label}>{t("newAnalysis.filters.endTime")}</label>
            <input
              type="datetime-local"
              className={styles.input}
              value={filters.endTime}
              disabled={disabled}
              onChange={(event) => updateFilter("endTime", event.target.value)}
            />
          </div>

          <div className={styles.field}>
            <label className={styles.label}>{t("newAnalysis.filters.levels")}</label>
            <div className={styles.levelChips}>
              {LEVEL_OPTIONS.map((level) => (
                <label key={level} className={styles.levelChip}>
                  <input
                    type="checkbox"
                    checked={filters.levels.includes(level)}
                    disabled={disabled}
                    onChange={() => toggleLevel(level)}
                  />
                  {level}
                </label>
              ))}
            </div>
          </div>

          {showLoggerThread && (
            <>
              <div className={styles.field}>
                <label className={styles.label}>{t("newAnalysis.filters.logger")}</label>
                <input
                  type="text"
                  className={styles.input}
                  value={filters.logger}
                  disabled={disabled}
                  onChange={(event) => updateFilter("logger", event.target.value)}
                />
              </div>

              <div className={styles.field}>
                <label className={styles.label}>{t("newAnalysis.filters.thread")}</label>
                <input
                  type="text"
                  className={styles.input}
                  value={filters.thread}
                  disabled={disabled}
                  onChange={(event) => updateFilter("thread", event.target.value)}
                />
              </div>
            </>
          )}

          <div className={styles.field}>
            <label className={styles.label}>{t("newAnalysis.filters.messageContains")}</label>
            <input
              type="text"
              className={styles.input}
              value={filters.messageContains}
              disabled={disabled}
              onChange={(event) => updateFilter("messageContains", event.target.value)}
            />
          </div>

          {showHttpFields && (
            <>
              <div className={styles.field}>
                <label className={styles.label}>{t("newAnalysis.filters.statusCodes")}</label>
                <input
                  type="text"
                  className={styles.input}
                  placeholder="404,500"
                  value={filters.statusCodes}
                  disabled={disabled}
                  onChange={(event) => updateFilter("statusCodes", event.target.value)}
                />
              </div>

              <div className={styles.field}>
                <label className={styles.label}>{t("newAnalysis.filters.httpMethods")}</label>
                <input
                  type="text"
                  className={styles.input}
                  placeholder="GET,POST"
                  value={filters.httpMethods}
                  disabled={disabled}
                  onChange={(event) => updateFilter("httpMethods", event.target.value)}
                />
              </div>

              <div className={styles.field}>
                <label className={styles.label}>{t("newAnalysis.filters.pathContains")}</label>
                <input
                  type="text"
                  className={styles.input}
                  value={filters.pathContains}
                  disabled={disabled}
                  onChange={(event) => updateFilter("pathContains", event.target.value)}
                />
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
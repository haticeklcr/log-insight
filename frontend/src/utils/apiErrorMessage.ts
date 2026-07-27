import type { TFunction } from "i18next";
import { LogAnalysisApiError } from "../services/logAnalysisApi";

export function translateApiError(error: unknown, t: TFunction, fallbackKey: string): string {
  if (error instanceof LogAnalysisApiError) {
    const key = `errors.${error.errorCode}`;
    const translated = t(key);
    return translated === key ? error.message : translated;
  }
  return t(fallbackKey);
}
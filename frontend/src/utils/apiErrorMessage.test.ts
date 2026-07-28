import { describe, it, expect, afterEach } from "vitest";
import i18n from "../i18n";
import { translateApiError } from "./apiErrorMessage";
import { LogAnalysisApiError } from "../services/logAnalysisApi";

describe("translateApiError - iki dilde hata kodu çevirisi", () => {
  afterEach(() => {
    i18n.changeLanguage("tr");
  });

  function apiError(errorCode: string, message: string) {
    return new LogAnalysisApiError({
      timestamp: new Date().toISOString(),
      status: 409,
      error: errorCode,
      message,
      path: "/api/v1/analysis-jobs/test-job-id",
    });
  }

  it("Türkçe'de bilinen bir errorCode için çevrilmiş mesajı döner", () => {
    i18n.changeLanguage("tr");

    const message = translateApiError(
      apiError("INVALID_JOB_STATE", "raw backend message"),
      i18n.t.bind(i18n),
      "newAnalysis.backendUnreachable"
    );

    expect(message).toBe("Bu iş, şu anki durumunda bu işlem için uygun değil");
  });

  it("İngilizce'de aynı errorCode için İngilizce çevrilmiş mesajı döner", () => {
    i18n.changeLanguage("en");

    const message = translateApiError(
      apiError("INVALID_JOB_STATE", "raw backend message"),
      i18n.t.bind(i18n),
      "newAnalysis.backendUnreachable"
    );

    expect(message).toBe("This job cannot perform this action in its current state");
  });

  it("bilinmeyen bir errorCode için backend'in ham message alanına düşer (fallback)", () => {
    i18n.changeLanguage("tr");

    const message = translateApiError(
      apiError("SOME_UNKNOWN_CODE", "backend'in ham mesajı"),
      i18n.t.bind(i18n),
      "newAnalysis.backendUnreachable"
    );

    expect(message).toBe("backend'in ham mesajı");
  });

  it("LogAnalysisApiError olmayan bir hata için verilen fallback anahtarı kullanır", () => {
    i18n.changeLanguage("tr");

    const message = translateApiError(new Error("network error"), i18n.t.bind(i18n), "newAnalysis.backendUnreachable");

    expect(message).toBe("Backend servisine ulaşılamadı. Lütfen daha sonra tekrar deneyin.");
  });
});
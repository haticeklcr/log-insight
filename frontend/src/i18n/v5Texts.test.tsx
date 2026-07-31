import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { render, screen } from "@testing-library/react";
import i18n from "./index";
import ParserAndFilterFields, {
  EMPTY_FILTER_STATE,
} from "../components/ParserAndFilterFields/ParserAndFilterFields";
import AnalysisDetailView from "../components/AnalysisDetailView/AnalysisDetailView";
import type { AnalysisDetail } from "../types/analysisHistory";
import { vi } from "vitest";

const DETAIL: AnalysisDetail = {
  id: 1,
  fileName: "app.log",
  analysisName: "Test",
  fileSize: 100,
  analyzedAt: "2026-01-01T10:00:00Z",
  processingDurationMs: 10,
  totalLines: 1,
  infoCount: 1,
  warningCount: 0,
  errorCount: 0,
  exceptionCount: 0,
  mostFrequentErrors: [],
  detectedLogFormat: "SPRING_BOOT",
  parseQualityScore: 90,
};

describe("V5 metinleri iki dilde de doğru gösteriliyor", () => {
  beforeEach(() => {
    i18n.changeLanguage("tr");
  });

  afterEach(() => {
    i18n.changeLanguage("tr");
  });

  it("parser seçimi ekranı Türkçe'de doğru metinleri gösterir", () => {
    render(
      <ParserAndFilterFields
        parserType="AUTO"
        onParserTypeChange={vi.fn()}
        filters={EMPTY_FILTER_STATE}
        onFiltersChange={vi.fn()}
        disabled={false}
      />
    );

    expect(screen.getByText("Log Formatı")).toBeInTheDocument();
    expect(screen.getByText("Gelişmiş Filtreleri Göster")).toBeInTheDocument();
  });

  it("parser seçimi ekranı İngilizce'de doğru metinleri gösterir", async () => {
    await i18n.changeLanguage("en");

    render(
      <ParserAndFilterFields
        parserType="AUTO"
        onParserTypeChange={vi.fn()}
        filters={EMPTY_FILTER_STATE}
        onFiltersChange={vi.fn()}
        disabled={false}
      />
    );

    expect(screen.getByText("Log Format")).toBeInTheDocument();
    expect(screen.getByText("Show Advanced Filters")).toBeInTheDocument();
  });

  it("sonuç ekranı Türkçe'de doğru metinleri gösterir", () => {
    render(<AnalysisDetailView detail={DETAIL} onBack={vi.fn()} />);

    expect(screen.getByText("Format Bilgisi")).toBeInTheDocument();
    expect(screen.getByText("Parse Kalite Skoru")).toBeInTheDocument();
  });

  it("sonuç ekranı İngilizce'de doğru metinleri gösterir", async () => {
    await i18n.changeLanguage("en");

    render(<AnalysisDetailView detail={DETAIL} onBack={vi.fn()} />);

    expect(screen.getByText("Format Information")).toBeInTheDocument();
    expect(screen.getByText("Parse Quality Score")).toBeInTheDocument();
  });
});
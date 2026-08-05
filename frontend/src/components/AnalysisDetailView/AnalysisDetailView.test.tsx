import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import AnalysisDetailView from "./AnalysisDetailView";
import type { AnalysisDetail } from "../../types/analysisHistory";

const BASE_DETAIL: AnalysisDetail = {
  id: 1,
  fileName: "app.log",
  analysisName: "Test Analizi",
  fileSize: 1024,
  analyzedAt: "2026-01-01T10:00:00Z",
  processingDurationMs: 120,
  totalLines: 10,
  infoCount: 5,
  warningCount: 2,
  errorCount: 3,
  exceptionCount: 1,
  mostFrequentErrors: [],
};

describe("AnalysisDetailView", () => {
  it("V5 alanları olmayan (eski) bir analiz için format bilgisi bölümünü göstermez", () => {
    render(<AnalysisDetailView detail={BASE_DETAIL} onBack={vi.fn()} />);

    expect(screen.queryByText("Format Bilgisi")).not.toBeInTheDocument();
  });

  it("algılanan format bilgisini gösterir", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      detectedLogFormat: "SPRING_BOOT",
      parseQualityScore: 95,
      formatConfidence: 100,
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.getByText("Format Bilgisi")).toBeInTheDocument();
    expect(screen.getByText("SPRING_BOOT")).toBeInTheDocument();
    expect(screen.getByText("95")).toBeInTheDocument();
  });

  it("algılanan envelope değerini gösterir", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      detectedLogFormat: "SPRING_BOOT",
      detectedEnvelope: "SYSLOG_RFC3164",
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.getByText("SYSLOG_RFC3164")).toBeInTheDocument();
  });

  it("envelope tespit edilmediğinde bunu belirten metni gösterir", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      detectedLogFormat: "SPRING_BOOT",
      detectedEnvelope: "NONE",
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.getByText("Envelope bulunamadı")).toBeInTheDocument();
  });

  it("zaman çizelgesi kademesini gösterir", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      detectedLogFormat: "SPRING_BOOT",
      timelineGranularity: "HOUR",
      timeline: [
        { bucketStart: "2026-01-01T10:00:00Z", totalCount: 5, infoCount: 3, warnCount: 1, errorCount: 1, exceptionCount: 0 },
      ],
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.getByText(/HOUR/)).toBeInTheDocument();
  });

  it("timeline verisi olduğunda zaman çizelgesi grafiğini gösterir", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      detectedLogFormat: "SPRING_BOOT",
      timeline: [
        { bucketStart: "2026-01-01T10:00:00Z", totalCount: 5, infoCount: 3, warnCount: 1, errorCount: 1, exceptionCount: 0 },
      ],
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.getByTestId("log-timeline-chart")).toBeInTheDocument();
  });

  it("logger istatistiği olduğunda logger tablosunu gösterir", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      detectedLogFormat: "SPRING_BOOT",
      mostFrequentLoggers: [{ loggerName: "com.example.PaymentService", count: 12 }],
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.getByText("com.example.PaymentService")).toBeInTheDocument();
    expect(screen.getByText("12")).toBeInTheDocument();
  });

  it("HTTP dağılımı verisi olmadığında ilgili bölümleri hiç göstermez", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      detectedLogFormat: "SPRING_BOOT",
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.queryByText("HTTP Status Dağılımı")).not.toBeInTheDocument();
    expect(screen.queryByText("HTTP Method Dağılımı")).not.toBeInTheDocument();
  });

  it("HTTP dağılımı verisi olduğunda status code ve method tablolarını gösterir", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      detectedLogFormat: "NGINX_ACCESS",
      statusCodeDistribution: [{ statusCode: 404, count: 7 }],
      httpMethodDistribution: [{ httpMethod: "GET", count: 20 }],
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.getByText("HTTP Status Dağılımı")).toBeInTheDocument();
    expect(screen.getByText("404")).toBeInTheDocument();
    expect(screen.getByText("HTTP Method Dağılımı")).toBeInTheDocument();
    expect(screen.getByText("GET")).toBeInTheDocument();
  });

  it("normalize edilmiş hata mesajını ve örnek ham mesajı birlikte gösterir", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      mostFrequentErrors: [
        { message: "User 98765 not found", normalizedMessage: "User <NUMBER> not found", count: 3 },
      ],
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.getByText("User <NUMBER> not found")).toBeInTheDocument();
    expect(screen.getByText("User 98765 not found")).toBeInTheDocument();
  });

  it("maskelenmiş bir mesajı olduğu gibi gösterir ve maskeleme notunu görüntüler", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      mostFrequentErrors: [
        { message: "Authorization: Bearer ****", normalizedMessage: "Authorization: Bearer ****", count: 1 },
      ],
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.getAllByText("Authorization: Bearer ****").length).toBeGreaterThan(0);
    expect(screen.getByText(/maskelenmiştir/)).toBeInTheDocument();
  });

  it("thread istatistiği olduğunda thread tablosunu gösterir", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      detectedLogFormat: "SPRING_BOOT",
      mostFrequentThreads: [{ threadName: "nio-8080-exec-1", count: 8 }],
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.getByText("nio-8080-exec-1")).toBeInTheDocument();
    expect(screen.getByText("8")).toBeInTheDocument();
  });

  it("seçilen parser, ilk/son log zamanı ve parse başarı yüzdesini gösterir", () => {
    const detail: AnalysisDetail = {
      ...BASE_DETAIL,
      requestedParserType: "SPRING_BOOT",
      detectedLogFormat: "SPRING_BOOT",
      firstLogTimestamp: "2026-01-01T10:00:00Z",
      lastLogTimestamp: "2026-01-01T11:00:00Z",
      unparsedLinePercentage: 2.5,
    };

    render(<AnalysisDetailView detail={detail} onBack={vi.fn()} />);

    expect(screen.getAllByText("SPRING_BOOT").length).toBe(2);
    expect(screen.getByText("%2.5")).toBeInTheDocument();
  });
});
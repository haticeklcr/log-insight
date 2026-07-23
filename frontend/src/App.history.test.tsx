import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";
import * as api from "./services/logAnalysisApi";

vi.mock("./services/logAnalysisApi", async () => {
  const actual = await vi.importActual<typeof import("./services/logAnalysisApi")>(
    "./services/logAnalysisApi"
  );
  return {
    ...actual,
    checkBackendHealth: vi.fn(),
    fetchAnalysisHistory: vi.fn(),
    fetchAnalysisDetail: vi.fn(),
  };
});

const mockedApi = api as unknown as {
  checkBackendHealth: ReturnType<typeof vi.fn>;
  fetchAnalysisHistory: ReturnType<typeof vi.fn>;
  fetchAnalysisDetail: ReturnType<typeof vi.fn>;
};

describe("App - Analiz Geçmişi navigasyonu", () => {
  beforeEach(() => {
    mockedApi.checkBackendHealth.mockResolvedValue(true);
    mockedApi.fetchAnalysisHistory.mockResolvedValue({
      content: [
        {
          id: 5,
          fileName: "history-item.log",
          fileSize: 1024,
          analyzedAt: "2026-01-01T10:00:00Z",
          totalLines: 20,
          errorCount: 3,
          exceptionCount: 0,
          processingDurationMs: 8,
        },
      ],
      page: 0,
      size: 10,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true,
    });
  });

  it("Analiz Geçmişi sekmesine geçip detay ekranını açabiliyor", async () => {
    mockedApi.fetchAnalysisDetail.mockResolvedValue({
      id: 5,
      fileName: "history-item.log",
      fileSize: 1024,
      analyzedAt: "2026-01-01T10:00:00Z",
      processingDurationMs: 8,
      totalLines: 20,
      infoCount: 15,
      warningCount: 2,
      errorCount: 3,
      exceptionCount: 0,
      mostFrequentErrors: [{ message: "Timeout", count: 2 }],
    });

    render(<App />);

    await userEvent.click(screen.getByRole("button", { name: "Analiz Geçmişi" }));

    await waitFor(() => expect(screen.getByText("history-item.log")).toBeInTheDocument());

    await userEvent.click(screen.getByRole("button", { name: "Detay" }));

    await waitFor(() => expect(mockedApi.fetchAnalysisDetail).toHaveBeenCalledWith(5));
    await waitFor(() => expect(screen.getByText("Timeout")).toBeInTheDocument());
  });
});
import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import App from "./App";
import * as api from "./services/logAnalysisApi";
import { LogAnalysisApiError } from "./services/logAnalysisApi";

vi.mock("./services/logAnalysisApi", async () => {
  const actual = await vi.importActual<typeof import("./services/logAnalysisApi")>(
    "./services/logAnalysisApi"
  );
  return {
    ...actual,
    analyzeLogFile: vi.fn(),
    checkBackendHealth: vi.fn(),
  };
});

const mockedApi = api as unknown as {
  analyzeLogFile: ReturnType<typeof vi.fn>;
  checkBackendHealth: ReturnType<typeof vi.fn>;
};

async function selectSampleFile() {
  const file = new File(["content"], "sample.log", { type: "text/plain" });
  const input = screen.getByTestId("file-input") as HTMLInputElement;
  await userEvent.upload(input, file);
}

describe("App", () => {
  beforeEach(() => {
    mockedApi.checkBackendHealth.mockResolvedValue(true);
    mockedApi.analyzeLogFile.mockReset();
  });

  it("API isteği sırasında loading göstergesini gösterir", async () => {
    let resolvePromise!: (value: unknown) => void;
    mockedApi.analyzeLogFile.mockReturnValue(
      new Promise((resolve) => {
        resolvePromise = resolve;
      })
    );

    render(<App />);
    await selectSampleFile();
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    expect(screen.getByText("Dosya analiz ediliyor...")).toBeInTheDocument();

    resolvePromise({
      fileName: "sample.log",
      totalLines: 1,
      infoCount: 1,
      warningCount: 0,
      errorCount: 0,
      exceptionCount: 0,
      mostFrequentErrors: [],
    });

    await waitFor(() =>
      expect(screen.queryByText("Dosya analiz ediliyor...")).not.toBeInTheDocument()
    );
  });

  it("başarılı cevapta özet kartlarını gösterir", async () => {
    mockedApi.analyzeLogFile.mockResolvedValue({
      fileName: "sample.log",
      totalLines: 9,
      infoCount: 3,
      warningCount: 1,
      errorCount: 4,
      exceptionCount: 1,
      mostFrequentErrors: [],
    });

    render(<App />);
    await selectSampleFile();
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    await waitFor(() => expect(screen.getByText("9")).toBeInTheDocument());
    expect(screen.getByText("Toplam Satır")).toBeInTheDocument();
    expect(screen.getByText("ERROR")).toBeInTheDocument();
  });

  it("sık hatalar tablosunu doğru oluşturur", async () => {
    mockedApi.analyzeLogFile.mockResolvedValue({
      fileName: "sample.log",
      totalLines: 9,
      infoCount: 5,
      warningCount: 2,
      errorCount: 8,
      exceptionCount: 6,
      mostFrequentErrors: [
        { message: "Connection refused", count: 3 },
        { message: "Request timeout", count: 1 },
      ],
    });

    render(<App />);
    await selectSampleFile();
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    await waitFor(() => expect(screen.getByText("Connection refused")).toBeInTheDocument());
    expect(screen.getByText("Request timeout")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
  });

  it("API hata cevabında kullanıcı dostu hata mesajını gösterir", async () => {
    mockedApi.analyzeLogFile.mockRejectedValue(
      new LogAnalysisApiError({
        timestamp: new Date().toISOString(),
        status: 400,
        error: "EMPTY_FILE",
        message: "Yüklenen dosya boş",
        path: "/api/v1/logs/analyze",
      })
    );

    render(<App />);
    await selectSampleFile();
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    await waitFor(() => expect(screen.getByText("Yüklenen dosya boş")).toBeInTheDocument());
  });
});
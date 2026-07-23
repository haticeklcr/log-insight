import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import HistoryView from "./HistoryView";
import * as api from "../../services/logAnalysisApi";

vi.mock("../../services/logAnalysisApi", async () => {
  const actual = await vi.importActual<typeof import("../../services/logAnalysisApi")>(
    "../../services/logAnalysisApi"
  );
  return {
    ...actual,
    fetchAnalysisHistory: vi.fn(),
    deleteAnalysis: vi.fn(),
  };
});

const mockedApi = api as unknown as {
  fetchAnalysisHistory: ReturnType<typeof vi.fn>;
  deleteAnalysis: ReturnType<typeof vi.fn>;
};

function samplePage(overrides = {}) {
  return {
    content: [
      {
        id: 1,
        fileName: "sample.log",
        fileSize: 450,
        analyzedAt: "2026-01-01T10:00:00Z",
        totalLines: 9,
        errorCount: 4,
        exceptionCount: 1,
        processingDurationMs: 12,
      },
    ],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
    ...overrides,
  };
}

describe("HistoryView", () => {
  const onViewDetail = vi.fn();

  beforeEach(() => {
    mockedApi.fetchAnalysisHistory.mockReset();
    mockedApi.deleteAnalysis.mockReset();
    onViewDetail.mockReset();
  });

  it("analiz geçmişi listesini yükler ve gösterir", async () => {
    mockedApi.fetchAnalysisHistory.mockResolvedValue(samplePage());

    render(<HistoryView onViewDetail={onViewDetail} />);

    await waitFor(() => expect(screen.getByText("sample.log")).toBeInTheDocument());
    expect(mockedApi.fetchAnalysisHistory).toHaveBeenCalled();
  });

  it("liste boşsa empty state gösterir", async () => {
    mockedApi.fetchAnalysisHistory.mockResolvedValue(samplePage({ content: [], totalElements: 0 }));

    render(<HistoryView onViewDetail={onViewDetail} />);

    await waitFor(() =>
      expect(screen.getByText("Henüz analiz geçmişi bulunmuyor.")).toBeInTheDocument()
    );
  });

  it("veri gelene kadar loading göstergesini gösterir", async () => {
    let resolvePromise!: (value: unknown) => void;
    mockedApi.fetchAnalysisHistory.mockReturnValue(
      new Promise((resolve) => {
        resolvePromise = resolve;
      })
    );

    render(<HistoryView onViewDetail={onViewDetail} />);

    expect(screen.getByText("Dosya analiz ediliyor...")).toBeInTheDocument();

    resolvePromise(samplePage());

    await waitFor(() =>
      expect(screen.queryByText("Dosya analiz ediliyor...")).not.toBeInTheDocument()
    );
  });

  it("API hata durumunda hata mesajını gösterir", async () => {
    mockedApi.fetchAnalysisHistory.mockRejectedValue(new Error("network error"));

    render(<HistoryView onViewDetail={onViewDetail} />);

    await waitFor(() =>
      expect(
        screen.getByText("Analiz geçmişi yüklenirken bir hata oluştu. Backend servisine ulaşılamıyor olabilir.")
      ).toBeInTheDocument()
    );
  });

  it("sonraki sayfa butonuna basınca yeni sayfa için istek atar", async () => {
    mockedApi.fetchAnalysisHistory.mockResolvedValue(
      samplePage({ totalElements: 20, totalPages: 2, last: false })
    );

    render(<HistoryView onViewDetail={onViewDetail} />);

    await waitFor(() => expect(screen.getByText("sample.log")).toBeInTheDocument());

    await userEvent.click(screen.getByRole("button", { name: "Sonraki Sayfa" }));

    await waitFor(() => {
      const lastCallArgs = mockedApi.fetchAnalysisHistory.mock.calls.at(-1)?.[0];
      expect(lastCallArgs.page).toBe(1);
    });
  });

  it("dosya adına göre arama yapıldığında filtreyle istek atar", async () => {
    mockedApi.fetchAnalysisHistory.mockResolvedValue(samplePage());

    render(<HistoryView onViewDetail={onViewDetail} />);

    await waitFor(() => expect(screen.getByText("sample.log")).toBeInTheDocument());

    await userEvent.type(screen.getByPlaceholderText("Dosya adına göre ara"), "prod");
    await userEvent.click(screen.getByRole("button", { name: "Uygula" }));

    await waitFor(() => {
      const lastCallArgs = mockedApi.fetchAnalysisHistory.mock.calls.at(-1)?.[0];
      expect(lastCallArgs.fileName).toBe("prod");
    });
  });

  it("detay butonuna basınca onViewDetail çağrılır", async () => {
    mockedApi.fetchAnalysisHistory.mockResolvedValue(samplePage());

    render(<HistoryView onViewDetail={onViewDetail} />);

    await waitFor(() => expect(screen.getByText("sample.log")).toBeInTheDocument());

    await userEvent.click(screen.getByRole("button", { name: "Detay" }));

    expect(onViewDetail).toHaveBeenCalledWith(1);
  });

  it("sil butonuna basınca onay penceresini gösterir", async () => {
    mockedApi.fetchAnalysisHistory.mockResolvedValue(samplePage());

    render(<HistoryView onViewDetail={onViewDetail} />);

    await waitFor(() => expect(screen.getByText("sample.log")).toBeInTheDocument());

    await userEvent.click(screen.getByRole("button", { name: "Sil" }));

    expect(
      screen.getByText((content, element) => element?.tagName === "P" && content.includes("silmek istediğinize"))
    ).toBeInTheDocument();
  });

  it("silme onaylanınca listeyi yeniler ve başarı mesajı gösterir", async () => {
    mockedApi.fetchAnalysisHistory.mockResolvedValue(samplePage());
    mockedApi.deleteAnalysis.mockResolvedValue(undefined);

    render(<HistoryView onViewDetail={onViewDetail} />);

    await waitFor(() => expect(screen.getByText("sample.log")).toBeInTheDocument());

    await userEvent.click(screen.getByRole("button", { name: "Sil" }));

    const silButtons = screen.getAllByRole("button", { name: "Sil" });
    await userEvent.click(silButtons[silButtons.length - 1]);

    await waitFor(() => expect(mockedApi.deleteAnalysis).toHaveBeenCalledWith(1));
    await waitFor(() =>
      expect(screen.getByText("Analiz kaydı başarıyla silindi")).toBeInTheDocument()
    );
    expect(mockedApi.fetchAnalysisHistory).toHaveBeenCalledTimes(2);
  });
});
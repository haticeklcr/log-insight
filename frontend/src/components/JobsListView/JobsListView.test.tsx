import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import JobsListView from "./JobsListView";
import * as api from "../../services/analysisJobApi";

vi.mock("../../services/analysisJobApi", async () => {
  const actual = await vi.importActual<typeof import("../../services/analysisJobApi")>(
    "../../services/analysisJobApi"
  );
  return {
    ...actual,
    fetchAnalysisJobs: vi.fn(),
    cancelAnalysisJob: vi.fn(),
    retryAnalysisJob: vi.fn(),
  };
});

const mockedApi = api as unknown as {
  fetchAnalysisJobs: ReturnType<typeof vi.fn>;
  cancelAnalysisJob: ReturnType<typeof vi.fn>;
  retryAnalysisJob: ReturnType<typeof vi.fn>;
};

function samplePage(overrides = {}, jobOverrides = {}) {
  return {
    content: [
      {
        jobId: "job-1",
        analysisName: "Test Analizi",
        fileName: "sample.log",
        status: "PENDING",
        progress: 0,
        retryCount: 0,
        createdAt: "2026-01-01T10:00:00Z",
        startedAt: null,
        completedAt: null,
        ...jobOverrides,
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

describe("JobsListView", () => {
  const onViewDetail = vi.fn();

  beforeEach(() => {
    mockedApi.fetchAnalysisJobs.mockReset();
    mockedApi.cancelAnalysisJob.mockReset();
    mockedApi.retryAnalysisJob.mockReset();
    onViewDetail.mockReset();
  });

  it("iş listesini yükler, analiz adını ve PENDING durumunu gösterir", async () => {
    mockedApi.fetchAnalysisJobs.mockResolvedValue(samplePage());

    render(<JobsListView onViewDetail={onViewDetail} />);

    await waitFor(() => expect(screen.getByText("Test Analizi")).toBeInTheDocument());
    expect(screen.getByText("Bekliyor", { selector: "span" })).toBeInTheDocument();
  });

  it("PENDING iş için İptal aktif, Retry ve Sonuç pasif olur", async () => {
    mockedApi.fetchAnalysisJobs.mockResolvedValue(samplePage());

    render(<JobsListView onViewDetail={onViewDetail} />);

    await waitFor(() => expect(screen.getByText("Test Analizi")).toBeInTheDocument());

    expect(screen.getByRole("button", { name: "İptal" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Retry" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Sonuç" })).toBeDisabled();
  });

  it("FAILED iş için yalnızca Retry aktif olur", async () => {
    mockedApi.fetchAnalysisJobs.mockResolvedValue(samplePage({}, { status: "FAILED" }));

    render(<JobsListView onViewDetail={onViewDetail} />);

    await waitFor(() => expect(screen.getByText("Test Analizi")).toBeInTheDocument());

    expect(screen.getByRole("button", { name: "İptal" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Retry" })).toBeEnabled();
    expect(screen.getByRole("button", { name: "Sonuç" })).toBeDisabled();
  });

  it("SUCCEEDED iş için yalnızca Sonuç aktif olur", async () => {
    mockedApi.fetchAnalysisJobs.mockResolvedValue(samplePage({}, { status: "SUCCEEDED", progress: 100 }));

    render(<JobsListView onViewDetail={onViewDetail} />);

    await waitFor(() => expect(screen.getByText("Test Analizi")).toBeInTheDocument());

    expect(screen.getByRole("button", { name: "İptal" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Retry" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Sonuç" })).toBeEnabled();
  });

  it("backend erişilemediğinde hata mesajı gösterir", async () => {
    mockedApi.fetchAnalysisJobs.mockRejectedValue(new Error("network error"));

    render(<JobsListView onViewDetail={onViewDetail} />);

    await waitFor(() =>
      expect(
        screen.getByText("İş listesi yüklenirken bir hata oluştu. Backend servisine ulaşılamıyor olabilir.")
      ).toBeInTheDocument()
    );
  });

  it("Detay butonuna basınca onViewDetail çağrılır", async () => {
    mockedApi.fetchAnalysisJobs.mockResolvedValue(samplePage());

    render(<JobsListView onViewDetail={onViewDetail} />);

    await waitFor(() => expect(screen.getByText("Test Analizi")).toBeInTheDocument());

    await userEvent.click(screen.getByRole("button", { name: "Detay" }));

    expect(onViewDetail).toHaveBeenCalledWith("job-1");
  });
});
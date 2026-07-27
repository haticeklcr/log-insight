import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import JobDetailView from "./JobDetailView";
import type { AnalysisJobDetail } from "../../types/analysisJob";

function sampleJob(overrides: Partial<AnalysisJobDetail> = {}): AnalysisJobDetail {
  return {
    jobId: "job-1",
    analysisName: "Test Analizi",
    fileName: "sample.log",
    fileSize: 450,
    status: "PENDING",
    progress: 0,
    retryCount: 0,
    createdAt: "2026-01-01T10:00:00Z",
    startedAt: null,
    completedAt: null,
    errorCode: null,
    analysisId: null,
    ...overrides,
  };
}

describe("JobDetailView", () => {
  it("analiz adını ve durumu gösterir", () => {
    render(
      <JobDetailView
        job={sampleJob()}
        pollingErrorMessage={null}
        onCancel={vi.fn()}
        onRetry={vi.fn()}
        onViewResult={vi.fn()}
        onBack={vi.fn()}
      />
    );

    expect(screen.getByText("Test Analizi")).toBeInTheDocument();
    expect(screen.getByText("Bekliyor")).toBeInTheDocument();
  });

  it("RUNNING durumunda progress bar gösterir", () => {
    render(
      <JobDetailView
        job={sampleJob({ status: "RUNNING", progress: 45, startedAt: "2026-01-01T10:00:01Z" })}
        pollingErrorMessage={null}
        onCancel={vi.fn()}
        onRetry={vi.fn()}
        onViewResult={vi.fn()}
        onBack={vi.fn()}
      />
    );

    expect(screen.getByText("%45")).toBeInTheDocument();
  });

  it("SUCCEEDED durumunda progress bar gösterilmez", () => {
    render(
      <JobDetailView
        job={sampleJob({ status: "SUCCEEDED", progress: 100, analysisId: 7 })}
        pollingErrorMessage={null}
        onCancel={vi.fn()}
        onRetry={vi.fn()}
        onViewResult={vi.fn()}
        onBack={vi.fn()}
      />
    );

    expect(screen.queryByText("%100")).not.toBeInTheDocument();
  });

  it("yalnızca FAILED durumda Tekrar Dene butonu gösterilir", () => {
    render(
      <JobDetailView
        job={sampleJob({ status: "FAILED", errorCode: "ANALYSIS_UNEXPECTED_ERROR" })}
        pollingErrorMessage={null}
        onCancel={vi.fn()}
        onRetry={vi.fn()}
        onViewResult={vi.fn()}
        onBack={vi.fn()}
      />
    );

    expect(screen.getByRole("button", { name: "Tekrar Dene" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "İptal Et" })).not.toBeInTheDocument();
  });

  it("SUCCEEDED job'dan Sonucu Görüntüle butonuna basınca onViewResult çağrılır", async () => {
    const onViewResult = vi.fn();
    render(
      <JobDetailView
        job={sampleJob({ status: "SUCCEEDED", progress: 100, analysisId: 7 })}
        pollingErrorMessage={null}
        onCancel={vi.fn()}
        onRetry={vi.fn()}
        onViewResult={onViewResult}
        onBack={vi.fn()}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: "Sonucu Görüntüle" }));

    expect(onViewResult).toHaveBeenCalledWith(7);
  });

  it("backend erişilemediğinde polling uyarısını gösterir", () => {
    render(
      <JobDetailView
        job={sampleJob({ status: "RUNNING" })}
        pollingErrorMessage="Backend servisine ulaşılamadı. İş durumu güncellenemiyor."
        onCancel={vi.fn()}
        onRetry={vi.fn()}
        onViewResult={vi.fn()}
        onBack={vi.fn()}
      />
    );

    expect(
      screen.getByText("Backend servisine ulaşılamadı. İş durumu güncellenemiyor.")
    ).toBeInTheDocument();
  });

  it("İptal Et butonuna basınca onCancel çağrılır", async () => {
    const onCancel = vi.fn();
    render(
      <JobDetailView
        job={sampleJob({ status: "RUNNING" })}
        pollingErrorMessage={null}
        onCancel={onCancel}
        onRetry={vi.fn()}
        onViewResult={vi.fn()}
        onBack={vi.fn()}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: "İptal Et" }));

    expect(onCancel).toHaveBeenCalled();
  });
});
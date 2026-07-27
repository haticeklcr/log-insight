import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { renderHook, waitFor, act } from "@testing-library/react";
import { useJobPolling } from "./useJobPolling";
import * as api from "../services/analysisJobApi";

vi.mock("../services/analysisJobApi", async () => {
  const actual = await vi.importActual<typeof import("../services/analysisJobApi")>(
    "../services/analysisJobApi"
  );
  return {
    ...actual,
    fetchAnalysisJob: vi.fn(),
  };
});

const mockedApi = api as unknown as {
  fetchAnalysisJob: ReturnType<typeof vi.fn>;
};

function job(overrides = {}) {
  return {
    jobId: "job-1",
    analysisName: "Test Analizi",
    fileName: "sample.log",
    fileSize: 10,
    status: "RUNNING",
    progress: 10,
    retryCount: 0,
    createdAt: "2026-01-01T10:00:00Z",
    startedAt: "2026-01-01T10:00:01Z",
    completedAt: null,
    errorCode: null,
    analysisId: null,
    ...overrides,
  };
}

describe("useJobPolling", () => {
  beforeEach(() => {
    mockedApi.fetchAnalysisJob.mockReset();
    vi.useFakeTimers({ shouldAdvanceTime: true });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("2 saniyede bir job durumunu sorgular", async () => {
    mockedApi.fetchAnalysisJob.mockResolvedValue(job());

    renderHook(() => useJobPolling("job-1"));

    await waitFor(() => expect(mockedApi.fetchAnalysisJob).toHaveBeenCalledTimes(1));

    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000);
    });
    expect(mockedApi.fetchAnalysisJob).toHaveBeenCalledTimes(2);

    await act(async () => {
      await vi.advanceTimersByTimeAsync(2000);
    });
    expect(mockedApi.fetchAnalysisJob).toHaveBeenCalledTimes(3);
  });

  it("terminal duruma ulaşınca polling durur", async () => {
    mockedApi.fetchAnalysisJob.mockResolvedValue(job({ status: "SUCCEEDED", progress: 100 }));

    renderHook(() => useJobPolling("job-1"));

    await waitFor(() => expect(mockedApi.fetchAnalysisJob).toHaveBeenCalledTimes(1));

    await act(async () => {
      await vi.advanceTimersByTimeAsync(6000);
    });

    expect(mockedApi.fetchAnalysisJob).toHaveBeenCalledTimes(1);
  });

  it("jobId null olduğunda hiç istek atmaz", () => {
    renderHook(() => useJobPolling(null));

    expect(mockedApi.fetchAnalysisJob).not.toHaveBeenCalled();
  });

  it("backend erişilemediğinde hata mesajı döner", async () => {
    mockedApi.fetchAnalysisJob.mockRejectedValue(new Error("network error"));

    const { result } = renderHook(() => useJobPolling("job-1"));

    await waitFor(() =>
      expect(result.current.errorMessage).toBe("Backend servisine ulaşılamadı. İş durumu güncellenemiyor.")
    );
  });

  it("unmount olduğunda interval temizlenir", async () => {
    mockedApi.fetchAnalysisJob.mockResolvedValue(job());

    const { unmount } = renderHook(() => useJobPolling("job-1"));

    await waitFor(() => expect(mockedApi.fetchAnalysisJob).toHaveBeenCalledTimes(1));

    unmount();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(6000);
    });

    expect(mockedApi.fetchAnalysisJob).toHaveBeenCalledTimes(1);
  });
});
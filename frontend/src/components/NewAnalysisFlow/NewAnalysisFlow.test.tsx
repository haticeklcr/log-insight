import { describe, it, expect, vi, beforeEach } from "vitest";
import { LogAnalysisApiError } from "../../services/logAnalysisApi";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import NewAnalysisFlow from "./NewAnalysisFlow";
import * as api from "../../services/analysisJobApi";
import * as uploadApi from "../../services/uploadApi";

vi.mock("../../services/analysisJobApi", async () => {
  const actual = await vi.importActual<typeof import("../../services/analysisJobApi")>(
    "../../services/analysisJobApi"
  );
  return {
    ...actual,
    createAnalysisJobFromUpload: vi.fn(),
  };
});

vi.mock("../../services/uploadApi", async () => {
  const actual = await vi.importActual<typeof import("../../services/uploadApi")>(
    "../../services/uploadApi"
  );
  return {
    ...actual,
    createUploadSession: vi.fn(),
    uploadChunk: vi.fn(),
    completeUpload: vi.fn(),
    getUploadStatus: vi.fn(),
  };
});

const mockedApi = api as unknown as {
  createAnalysisJobFromUpload: ReturnType<typeof vi.fn>;
};

const mockedUploadApi = uploadApi as unknown as {
  createUploadSession: ReturnType<typeof vi.fn>;
  uploadChunk: ReturnType<typeof vi.fn>;
  completeUpload: ReturnType<typeof vi.fn>;
  getUploadStatus: ReturnType<typeof vi.fn>;
};

function sampleFile() {
  return new File(["log content"], "sample.log", { type: "text/plain" });
}

function mockSuccessfulUploadOfOneChunk() {
  mockedUploadApi.createUploadSession.mockResolvedValue({
    uploadId: "upload-1",
    chunkSize: 8 * 1024 * 1024,
    totalChunks: 1,
    parallelism: 4,
    expiresAt: "2026-01-02T10:00:00Z",
  });
  mockedUploadApi.uploadChunk.mockResolvedValue(undefined);
  mockedUploadApi.completeUpload.mockResolvedValue(undefined);
  mockedUploadApi.getUploadStatus.mockResolvedValue({
    uploadId: "upload-1",
    fileName: "sample.log",
    fileSize: 11,
    chunkSize: 8 * 1024 * 1024,
    totalChunks: 1,
    receivedCount: 1,
    missingChunks: [],
    status: "COMPLETED",
    mergeProgress: 100,
    expiresAt: "2026-01-02T10:00:00Z",
  });
}

describe("NewAnalysisFlow", () => {
  const onJobCreated = vi.fn();

  beforeEach(() => {
    mockedApi.createAnalysisJobFromUpload.mockReset();
    mockedUploadApi.createUploadSession.mockReset();
    mockedUploadApi.uploadChunk.mockReset();
    mockedUploadApi.completeUpload.mockReset();
    mockedUploadApi.getUploadStatus.mockReset();
    onJobCreated.mockReset();
    localStorage.clear();
  });

  it("analiz adı boş bırakıldığında validation mesajı gösterir ve job oluşturulmaz", async () => {
    render(<NewAnalysisFlow onJobCreated={onJobCreated} />);

    await userEvent.upload(screen.getByTestId("file-input"), sampleFile());
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    expect(screen.getByText("Analiz adı zorunludur")).toBeInTheDocument();
    expect(mockedUploadApi.createUploadSession).not.toHaveBeenCalled();
  });

  it("analiz adı 3 karakterden kısa olduğunda validation mesajı gösterir", async () => {
    render(<NewAnalysisFlow onJobCreated={onJobCreated} />);

    await userEvent.type(screen.getByTestId("analysis-name-input"), "ab");
    await userEvent.upload(screen.getByTestId("file-input"), sampleFile());
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    expect(screen.getByText("Analiz adı en az 3 karakter olmalıdır")).toBeInTheDocument();
    expect(mockedUploadApi.createUploadSession).not.toHaveBeenCalled();
  });

  it("geçerli analiz adı ve dosya ile job oluşturur ve onJobCreated çağrılır", async () => {
    mockSuccessfulUploadOfOneChunk();
    mockedApi.createAnalysisJobFromUpload.mockResolvedValue({
      jobId: "abc-123",
      analysisName: "Test Analizi",
      status: "PENDING",
      progress: 0,
      createdAt: "2026-01-01T10:00:00Z",
    });

    render(<NewAnalysisFlow onJobCreated={onJobCreated} />);

    await userEvent.type(screen.getByTestId("analysis-name-input"), "Test Analizi");
    await userEvent.upload(screen.getByTestId("file-input"), sampleFile());
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    await waitFor(() => expect(onJobCreated).toHaveBeenCalledWith("abc-123"), { timeout: 3000 });
    expect(mockedUploadApi.createUploadSession).toHaveBeenCalledWith("sample.log", 11);
    expect(mockedUploadApi.uploadChunk).toHaveBeenCalledWith("upload-1", 0, expect.anything());
    expect(mockedUploadApi.completeUpload).toHaveBeenCalledWith("upload-1");
    expect(mockedApi.createAnalysisJobFromUpload).toHaveBeenCalledWith(
      "upload-1", "Test Analizi", undefined);
  });

  it("baştaki ve sondaki boşlukları temizleyerek gönderir", async () => {
    mockSuccessfulUploadOfOneChunk();
    mockedApi.createAnalysisJobFromUpload.mockResolvedValue({
      jobId: "abc-123",
      analysisName: "Test Analizi",
      status: "PENDING",
      progress: 0,
      createdAt: "2026-01-01T10:00:00Z",
    });

    render(<NewAnalysisFlow onJobCreated={onJobCreated} />);

    await userEvent.type(screen.getByTestId("analysis-name-input"), "  Test Analizi  ");
    await userEvent.upload(screen.getByTestId("file-input"), sampleFile());
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    await waitFor(() => expect(mockedApi.createAnalysisJobFromUpload).toHaveBeenCalledWith(
      "upload-1", "Test Analizi", undefined), { timeout: 3000 });
  });

  it("manuel parser seçimi yapıldığında parserType seçenekle birlikte gönderilir", async () => {
    mockSuccessfulUploadOfOneChunk();
    mockedApi.createAnalysisJobFromUpload.mockResolvedValue({
      jobId: "abc-123",
      analysisName: "Test Analizi",
      status: "PENDING",
      progress: 0,
      createdAt: "2026-01-01T10:00:00Z",
    });

    render(<NewAnalysisFlow onJobCreated={onJobCreated} />);

    await userEvent.type(screen.getByTestId("analysis-name-input"), "Test Analizi");
    await userEvent.selectOptions(screen.getByTestId("parser-type-select"), "JSON");
    await userEvent.upload(screen.getByTestId("file-input"), sampleFile());
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    await waitFor(() => expect(mockedApi.createAnalysisJobFromUpload).toHaveBeenCalledWith(
      "upload-1",
      "Test Analizi",
      expect.objectContaining({ parserType: "JSON" }),
    ), { timeout: 3000 });
  });

  it("gelişmiş filtre alanına girilen değerler seçenekle birlikte gönderilir", async () => {
    mockSuccessfulUploadOfOneChunk();
    mockedApi.createAnalysisJobFromUpload.mockResolvedValue({
      jobId: "abc-123",
      analysisName: "Test Analizi",
      status: "PENDING",
      progress: 0,
      createdAt: "2026-01-01T10:00:00Z",
    });

    render(<NewAnalysisFlow onJobCreated={onJobCreated} />);

    await userEvent.type(screen.getByTestId("analysis-name-input"), "Test Analizi");
    await userEvent.click(screen.getByTestId("toggle-advanced-filters"));
    await userEvent.click(screen.getByLabelText("ERROR"));
    await userEvent.upload(screen.getByTestId("file-input"), sampleFile());
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    await waitFor(() => expect(mockedApi.createAnalysisJobFromUpload).toHaveBeenCalledWith(
      "upload-1",
      "Test Analizi",
      expect.objectContaining({ levels: ["ERROR"] }),
    ), { timeout: 3000 });
  });

  it("geçersiz tarih aralığı backend'den dönünce hata mesajını gösterir", async () => {
    mockSuccessfulUploadOfOneChunk();
    mockedApi.createAnalysisJobFromUpload.mockRejectedValue(
      new LogAnalysisApiError({
        error: "INVALID_DATE_RANGE",
        message: "endTime, startTime'dan sonra olmalidir",
        timestamp: "2026-01-01T10:00:00Z",
        status: 400,
        path: "/api/v1/analysis-jobs",
      })
    );

    render(<NewAnalysisFlow onJobCreated={onJobCreated} />);

    await userEvent.type(screen.getByTestId("analysis-name-input"), "Test Analizi");
    await userEvent.upload(screen.getByTestId("file-input"), sampleFile());
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    expect(await screen.findByText("Geçersiz tarih aralığı", {}, { timeout: 3000 })).toBeInTheDocument();
  });
});
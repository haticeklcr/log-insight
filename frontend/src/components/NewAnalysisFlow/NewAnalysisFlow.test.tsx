import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import NewAnalysisFlow from "./NewAnalysisFlow";
import * as api from "../../services/analysisJobApi";

vi.mock("../../services/analysisJobApi", async () => {
  const actual = await vi.importActual<typeof import("../../services/analysisJobApi")>(
    "../../services/analysisJobApi"
  );
  return {
    ...actual,
    createAnalysisJob: vi.fn(),
  };
});

const mockedApi = api as unknown as {
  createAnalysisJob: ReturnType<typeof vi.fn>;
};

function sampleFile() {
  return new File(["log content"], "sample.log", { type: "text/plain" });
}

describe("NewAnalysisFlow", () => {
  const onJobCreated = vi.fn();

  beforeEach(() => {
    mockedApi.createAnalysisJob.mockReset();
    onJobCreated.mockReset();
  });

  it("analiz adı boş bırakıldığında validation mesajı gösterir ve job oluşturulmaz", async () => {
    render(<NewAnalysisFlow onJobCreated={onJobCreated} />);

    await userEvent.upload(screen.getByTestId("file-input"), sampleFile());
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    expect(screen.getByText("Analiz adı zorunludur")).toBeInTheDocument();
    expect(mockedApi.createAnalysisJob).not.toHaveBeenCalled();
  });

  it("analiz adı 3 karakterden kısa olduğunda validation mesajı gösterir", async () => {
    render(<NewAnalysisFlow onJobCreated={onJobCreated} />);

    await userEvent.type(screen.getByTestId("analysis-name-input"), "ab");
    await userEvent.upload(screen.getByTestId("file-input"), sampleFile());
    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    expect(screen.getByText("Analiz adı en az 3 karakter olmalıdır")).toBeInTheDocument();
    expect(mockedApi.createAnalysisJob).not.toHaveBeenCalled();
  });

  it("geçerli analiz adı ve dosya ile job oluşturur ve onJobCreated çağrılır", async () => {
    mockedApi.createAnalysisJob.mockResolvedValue({
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

    expect(mockedApi.createAnalysisJob).toHaveBeenCalledWith(expect.any(File), "Test Analizi");
    expect(onJobCreated).toHaveBeenCalledWith("abc-123");
  });

  it("baştaki ve sondaki boşlukları temizleyerek gönderir", async () => {
    mockedApi.createAnalysisJob.mockResolvedValue({
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

    expect(mockedApi.createAnalysisJob).toHaveBeenCalledWith(expect.any(File), "Test Analizi");
  });

  it("manuel parser seçimi yapıldığında parserType seçenekle birlikte gönderilir", async () => {
    mockedApi.createAnalysisJob.mockResolvedValue({
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

    expect(mockedApi.createAnalysisJob).toHaveBeenCalledWith(
      expect.any(File),
      "Test Analizi",
      expect.objectContaining({ parserType: "JSON" }),
    );
  });

  it("gelişmiş filtre alanına girilen değerler seçenekle birlikte gönderilir", async () => {
    mockedApi.createAnalysisJob.mockResolvedValue({
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

    expect(mockedApi.createAnalysisJob).toHaveBeenCalledWith(
      expect.any(File),
      "Test Analizi",
      expect.objectContaining({ levels: ["ERROR"] }),
    );
  });
});
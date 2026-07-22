import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import FileUpload from "./FileUpload";

describe("FileUpload", () => {
  it("dosya seçilmeden Analiz Et butonuna basılınca hata gösterir ve onAnalyze çağrılmaz", async () => {
    const onAnalyze = vi.fn();
    render(<FileUpload onAnalyze={onAnalyze} isLoading={false} />);

    await userEvent.click(screen.getByRole("button", { name: "Analiz Et" }));

    expect(screen.getByText("Lütfen önce bir dosya seçin")).toBeInTheDocument();
    expect(onAnalyze).not.toHaveBeenCalled();
  });

  it("geçerli bir dosya seçildiğinde dosya adını gösterir", async () => {
    const onAnalyze = vi.fn();
    render(<FileUpload onAnalyze={onAnalyze} isLoading={false} />);

    const file = new File(["log content"], "sample.log", { type: "text/plain" });
    const input = screen.getByTestId("file-input") as HTMLInputElement;

    await userEvent.upload(input, file);

    expect(screen.getByText("sample.log")).toBeInTheDocument();
  });
});
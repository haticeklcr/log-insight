import { describe, it, expect, afterEach } from "vitest";
import { render, screen } from "@testing-library/react";
import i18n from "../../i18n";
import JobStatusBadge from "./JobStatusBadge";

describe("JobStatusBadge - iki dilde durum gösterimi", () => {
  afterEach(() => {
    i18n.changeLanguage("tr");
  });

  it("Türkçe'de RUNNING durumunu 'Çalışıyor' olarak gösterir", () => {
    i18n.changeLanguage("tr");
    render(<JobStatusBadge status="RUNNING" />);

    expect(screen.getByText("Çalışıyor")).toBeInTheDocument();
  });

  it("İngilizce'de RUNNING durumunu 'Running' olarak gösterir", () => {
    i18n.changeLanguage("en");
    render(<JobStatusBadge status="RUNNING" />);

    expect(screen.getByText("Running")).toBeInTheDocument();
  });

  it("Türkçe'de FAILED durumunu 'Başarısız' olarak gösterir", () => {
    i18n.changeLanguage("tr");
    render(<JobStatusBadge status="FAILED" />);

    expect(screen.getByText("Başarısız")).toBeInTheDocument();
  });

  it("İngilizce'de FAILED durumunu 'Failed' olarak gösterir", () => {
    i18n.changeLanguage("en");
    render(<JobStatusBadge status="FAILED" />);

    expect(screen.getByText("Failed")).toBeInTheDocument();
  });
});
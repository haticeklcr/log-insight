import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ParserAndFilterFields, { EMPTY_FILTER_STATE } from "./ParserAndFilterFields";

describe("ParserAndFilterFields", () => {
  it("varsayılan parser seçimi Otomatik Algıla'dır", () => {
    render(
      <ParserAndFilterFields
        parserType="AUTO"
        onParserTypeChange={vi.fn()}
        filters={EMPTY_FILTER_STATE}
        onFiltersChange={vi.fn()}
        disabled={false}
      />
    );

    expect(screen.getByTestId("parser-type-select")).toHaveValue("AUTO");
  });

  it("parser seçimi değiştirildiğinde onParserTypeChange çağrılır", async () => {
    const onParserTypeChange = vi.fn();
    render(
      <ParserAndFilterFields
        parserType="AUTO"
        onParserTypeChange={onParserTypeChange}
        filters={EMPTY_FILTER_STATE}
        onFiltersChange={vi.fn()}
        disabled={false}
      />
    );

    await userEvent.selectOptions(screen.getByTestId("parser-type-select"), "SPRING_BOOT");

    expect(onParserTypeChange).toHaveBeenCalledWith("SPRING_BOOT");
  });

  it("gelişmiş filtreler varsayılan olarak gizlidir, butona basınca gösterilir", async () => {
    render(
      <ParserAndFilterFields
        parserType="AUTO"
        onParserTypeChange={vi.fn()}
        filters={EMPTY_FILTER_STATE}
        onFiltersChange={vi.fn()}
        disabled={false}
      />
    );

    expect(screen.queryByTestId("advanced-filters-panel")).not.toBeInTheDocument();

    await userEvent.click(screen.getByTestId("toggle-advanced-filters"));

    expect(screen.getByTestId("advanced-filters-panel")).toBeInTheDocument();
  });

  it("Spring Boot seçiliyken logger/thread alanları gösterilir, HTTP alanları gösterilmez", async () => {
    render(
      <ParserAndFilterFields
        parserType="SPRING_BOOT"
        onParserTypeChange={vi.fn()}
        filters={EMPTY_FILTER_STATE}
        onFiltersChange={vi.fn()}
        disabled={false}
      />
    );

    await userEvent.click(screen.getByTestId("toggle-advanced-filters"));

    expect(screen.getByText("Logger")).toBeInTheDocument();
    expect(screen.getByText("Thread")).toBeInTheDocument();
    expect(screen.queryByText(/HTTP Status Code/)).not.toBeInTheDocument();
  });

  it("Nginx Access Log seçiliyken HTTP alanları gösterilir, logger/thread gösterilmez", async () => {
    render(
      <ParserAndFilterFields
        parserType="NGINX_ACCESS"
        onParserTypeChange={vi.fn()}
        filters={EMPTY_FILTER_STATE}
        onFiltersChange={vi.fn()}
        disabled={false}
      />
    );

    await userEvent.click(screen.getByTestId("toggle-advanced-filters"));

    expect(screen.getByText(/HTTP Status Code/)).toBeInTheDocument();
    expect(screen.getByText(/HTTP Method/)).toBeInTheDocument();
    expect(screen.queryByText("Logger")).not.toBeInTheDocument();
  });

  it("bir log seviyesi işaretlendiğinde onFiltersChange güncellenmiş levels ile çağrılır", async () => {
    const onFiltersChange = vi.fn();
    render(
      <ParserAndFilterFields
        parserType="AUTO"
        onParserTypeChange={vi.fn()}
        filters={EMPTY_FILTER_STATE}
        onFiltersChange={onFiltersChange}
        disabled={false}
      />
    );

    await userEvent.click(screen.getByTestId("toggle-advanced-filters"));
    await userEvent.click(screen.getByLabelText("ERROR"));

    expect(onFiltersChange).toHaveBeenCalledWith({ ...EMPTY_FILTER_STATE, levels: ["ERROR"] });
  });
});
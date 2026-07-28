import { describe, it, expect, beforeEach, afterEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import i18n from "../../i18n";
import LanguageSwitcher from "./LanguageSwitcher";

const STORAGE_KEY = "log-insight-language";

describe("LanguageSwitcher", () => {
  beforeEach(() => {
    window.localStorage.removeItem(STORAGE_KEY);
    i18n.changeLanguage("tr");
  });

  afterEach(() => {
    window.localStorage.removeItem(STORAGE_KEY);
    i18n.changeLanguage("tr");
  });

  it("EN butonuna basınca dili İngilizce'ye çevirir", async () => {
    render(<LanguageSwitcher />);

    await userEvent.click(screen.getByRole("button", { name: "EN" }));

    expect(i18n.language).toBe("en");
  });

  it("TR butonuna basınca dili Türkçe'ye çevirir", async () => {
    render(<LanguageSwitcher />);

    await userEvent.click(screen.getByRole("button", { name: "EN" }));
    await userEvent.click(screen.getByRole("button", { name: "TR" }));

    expect(i18n.language).toBe("tr");
  });

  it("dil değişikliğini localStorage'a kaydeder", async () => {
    render(<LanguageSwitcher />);

    await userEvent.click(screen.getByRole("button", { name: "EN" }));

    expect(window.localStorage.getItem(STORAGE_KEY)).toBe("en");
  });
});
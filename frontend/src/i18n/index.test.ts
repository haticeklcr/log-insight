import { describe, it, expect, beforeEach } from "vitest";
import { resolveInitialLanguage } from "./index";

const STORAGE_KEY = "log-insight-language";

describe("i18n dil tercihinin kalıcılığı", () => {
  beforeEach(() => {
    window.localStorage.removeItem(STORAGE_KEY);
  });

  it("localStorage'da hiç değer yokken varsayılan dili (tr) kullanır", () => {
    expect(resolveInitialLanguage()).toBe("tr");
  });

  it("localStorage'da 'en' varsa onu kullanır (sayfa yenilenince tercih korunur)", () => {
    window.localStorage.setItem(STORAGE_KEY, "en");

    expect(resolveInitialLanguage()).toBe("en");
  });

  it("localStorage'da desteklenmeyen bir dil varsa fallback (tr) dile döner", () => {
    window.localStorage.setItem(STORAGE_KEY, "de");

    expect(resolveInitialLanguage()).toBe("tr");
  });
});
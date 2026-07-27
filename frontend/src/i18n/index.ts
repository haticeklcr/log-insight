import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import tr from "./locales/tr.json";
import en from "./locales/en.json";

export const SUPPORTED_LANGUAGES = ["tr", "en"] as const;
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];
export const DEFAULT_LANGUAGE: SupportedLanguage = "tr";

const LANGUAGE_STORAGE_KEY = "log-insight-language";

function resolveInitialLanguage(): SupportedLanguage {
  const stored = window.localStorage.getItem(LANGUAGE_STORAGE_KEY);
  if (stored && (SUPPORTED_LANGUAGES as readonly string[]).includes(stored)) {
    return stored as SupportedLanguage;
  }
  return DEFAULT_LANGUAGE;
}

i18n.use(initReactI18next).init({
  resources: {
    tr: { translation: tr },
    en: { translation: en },
  },
  lng: resolveInitialLanguage(),
  fallbackLng: DEFAULT_LANGUAGE,
  interpolation: {
    escapeValue: false,
  },
});

i18n.on("languageChanged", (lng) => {
  window.localStorage.setItem(LANGUAGE_STORAGE_KEY, lng);
});

export default i18n;
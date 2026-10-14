import i18n from 'i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import { initReactI18next } from 'react-i18next';
import authEn from './locales/en/auth.json';
import boardsEn from './locales/en/boards.json';
import commonEn from './locales/en/common.json';
import communityEn from './locales/en/community.json';
import homeEn from './locales/en/home.json';
import tracksEn from './locales/en/tracks.json';
import authKo from './locales/ko/auth.json';
import boardsKo from './locales/ko/boards.json';
import commonKo from './locales/ko/common.json';
import communityKo from './locales/ko/community.json';
import homeKo from './locales/ko/home.json';
import tracksKo from './locales/ko/tracks.json';
import {
  FALLBACK_LANGUAGE,
  LANGUAGE_STORAGE_KEY,
  SUPPORTED_LANGUAGES,
  getBaseLanguage,
} from './languages.js';

export const namespaces = ['common', 'auth', 'home', 'tracks', 'boards', 'community'];

export const resources = {
  en: {
    common: commonEn,
    auth: authEn,
    home: homeEn,
    tracks: tracksEn,
    boards: boardsEn,
    community: communityEn,
  },
  ko: {
    common: commonKo,
    auth: authKo,
    home: homeKo,
    tracks: tracksKo,
    boards: boardsKo,
    community: communityKo,
  },
};

function syncDocumentLanguage(language) {
  if (typeof document !== 'undefined') {
    document.documentElement.lang = getBaseLanguage(language);
  }
}

if (!i18n.isInitialized) {
  i18n
    .use(LanguageDetector)
    .use(initReactI18next)
    .init({
      resources,
      ns: namespaces,
      defaultNS: 'common',
      supportedLngs: SUPPORTED_LANGUAGES,
      fallbackLng: FALLBACK_LANGUAGE,
      load: 'languageOnly',
      cleanCode: true,
      detection: {
        order: ['localStorage', 'navigator'],
        lookupLocalStorage: LANGUAGE_STORAGE_KEY,
        caches: ['localStorage'],
        convertDetectedLanguage: getBaseLanguage,
      },
      interpolation: {
        escapeValue: false,
      },
      react: {
        useSuspense: false,
      },
    });

  i18n.on('languageChanged', syncDocumentLanguage);
}

syncDocumentLanguage(i18n.resolvedLanguage ?? i18n.language);

export default i18n;

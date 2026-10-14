import LanguageDetector from 'i18next-browser-languagedetector';
import { beforeEach, describe, expect, it } from 'vitest';
import { LANGUAGE_STORAGE_KEY, getBaseLanguage } from './languages.js';

describe('language detection', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('prefers the saved language over the browser language', () => {
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, 'ko');
    const detector = new LanguageDetector(undefined, {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: LANGUAGE_STORAGE_KEY,
      convertDetectedLanguage: getBaseLanguage,
    });

    expect(detector.detect()).toBe('ko');
  });
});

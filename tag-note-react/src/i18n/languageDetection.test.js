import LanguageDetector from 'i18next-browser-languagedetector';
import { beforeEach, describe, expect, it } from 'vitest';
import { LANGUAGE_STORAGE_KEY, getBaseLanguage } from './languages.js';

describe('language detection', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('uses the browser language even when a language was saved previously', () => {
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, 'ko');
    const detector = new LanguageDetector(undefined, {
      order: ['navigator'],
      convertDetectedLanguage: getBaseLanguage,
    });

    expect(detector.detect()).toBe(getBaseLanguage(navigator.language));
  });
});

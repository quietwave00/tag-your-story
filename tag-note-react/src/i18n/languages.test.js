import { describe, expect, it } from 'vitest';
import { FALLBACK_LANGUAGE, getBaseLanguage } from './languages.js';

describe('getBaseLanguage', () => {
  it.each([
    ['ko', 'ko'],
    ['ko-KR', 'ko'],
    ['en', 'en'],
    ['en-US', 'en'],
  ])('maps %s to %s', (language, expected) => {
    expect(getBaseLanguage(language)).toBe(expected);
  });

  it('uses the fallback for unsupported languages', () => {
    expect(getBaseLanguage('ja-JP')).toBe(FALLBACK_LANGUAGE);
  });
});

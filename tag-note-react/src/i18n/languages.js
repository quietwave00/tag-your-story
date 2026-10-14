export const SUPPORTED_LANGUAGES = ['ko', 'en'];
export const FALLBACK_LANGUAGE = 'en';
export const LANGUAGE_STORAGE_KEY = 'tagnote.language';

export const languageOptions = [
  { code: 'ko', shortLabel: 'KO', labelKey: 'language.korean' },
  { code: 'en', shortLabel: 'EN', labelKey: 'language.english' },
];

export function getBaseLanguage(language) {
  const baseLanguage = language?.toLowerCase().split('-')[0];
  return SUPPORTED_LANGUAGES.includes(baseLanguage) ? baseLanguage : FALLBACK_LANGUAGE;
}

import { useTranslation } from 'react-i18next';
import { getBaseLanguage, languageOptions } from '../../i18n/languages.js';

export default function LanguageSwitcher() {
  const { i18n, t } = useTranslation('common');
  const currentLanguage = getBaseLanguage(i18n.resolvedLanguage ?? i18n.language);

  return (
    <div className="language-switcher" aria-label={t('language.label')} role="group">
      {languageOptions.map((option) => (
        <button
          aria-label={t(option.labelKey)}
          aria-pressed={currentLanguage === option.code}
          className="language-option"
          key={option.code}
          onClick={() => i18n.changeLanguage(option.code)}
          type="button"
        >
          {option.shortLabel}
        </button>
      ))}
    </div>
  );
}

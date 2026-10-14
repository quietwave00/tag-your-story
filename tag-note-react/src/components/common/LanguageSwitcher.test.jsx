import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/index.js';
import { LANGUAGE_STORAGE_KEY } from '../../i18n/languages.js';
import LanguageSwitcher from './LanguageSwitcher.jsx';

describe('LanguageSwitcher', () => {
  beforeEach(async () => {
    window.localStorage.clear();
    await i18n.changeLanguage('en');
  });

  afterEach(() => {
    cleanup();
  });

  it('changes the UI language and persists the explicit choice', async () => {
    render(<LanguageSwitcher />);

    fireEvent.click(screen.getByRole('button', { name: 'Korean' }));

    expect(await screen.findByRole('button', { name: '한국어' })).toHaveAttribute('aria-pressed', 'true');
    expect(document.documentElement).toHaveAttribute('lang', 'ko');
    expect(window.localStorage.getItem(LANGUAGE_STORAGE_KEY)).toBe('ko');
  });

  it('does not change dynamic content when the locale changes', async () => {
    const trackTitle = 'Paranoid Android';
    const userTag = 'shoegaze';
    const { rerender } = render(
      <div>
        <LanguageSwitcher />
        <span>{trackTitle}</span>
        <span>#{userTag}</span>
      </div>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'Korean' }));
    rerender(
      <div>
        <LanguageSwitcher />
        <span>{trackTitle}</span>
        <span>#{userTag}</span>
      </div>,
    );

    expect(screen.getByText(trackTitle)).toBeInTheDocument();
    expect(screen.getByText(`#${userTag}`)).toBeInTheDocument();
  });
});

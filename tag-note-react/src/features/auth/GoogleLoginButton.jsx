import { useTranslation } from 'react-i18next';

export default function GoogleLoginButton({ disabled = false, onClick }) {
  const { t } = useTranslation('auth');

  return (
    <button className="auth-primary-button login-google-button" disabled={disabled} onClick={onClick} type="button">
      <span className="login-google-mark" aria-hidden="true">
        <svg viewBox="0 0 24 24" focusable="false">
          <path d="M21.4 12.2c0-.7-.1-1.4-.2-2H12v3.8h5.2a4.5 4.5 0 0 1-1.9 2.9v2.5h3.1c1.9-1.7 3-4.2 3-7.2ZM12 21c2.7 0 4.9-.9 6.5-2.5l-3.1-2.4c-.9.6-2 .9-3.4.9-2.6 0-4.8-1.8-5.6-4.2H3.2v2.6A9 9 0 0 0 12 21ZM6.4 12.8a5.4 5.4 0 0 1 0-3.6V6.6H3.2a9 9 0 0 0 0 8.8l3.2-2.6ZM12 7c1.5 0 2.8.5 3.8 1.5l2.8-2.8A9 9 0 0 0 3.2 6.6l3.2 2.6C7.2 8.8 9.4 7 12 7Z" />
        </svg>
      </span>
      <span>{t('login.google')}</span>
    </button>
  );
}

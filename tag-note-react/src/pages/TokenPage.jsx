import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate } from 'react-router-dom';
import { authStore } from '../store/authStore.js';
import { routes } from '../utils/routes.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import '../styles/auth.css';

export default function TokenPage() {
  const { t } = useTranslation('auth');
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    if (params.has('error_code')) {
      const errorCode = params.get('error_code');
      authStore.clear();
      const knownCodes = new Set([
        'GOOGLE_EMAIL_UNVERIFIED',
        'GOOGLE_IDENTITY_INVALID',
        'GOOGLE_EMAIL_CONFLICT_LOCAL',
        'GOOGLE_EMAIL_CONFLICT_GOOGLE',
        'USER_INACTIVE',
        'GOOGLE_SIGNUP_CONFLICT',
      ]);
      navigate(routes.login, {
        replace: true,
        state: { messageKey: knownCodes.has(errorCode) ? `oauth.${errorCode}` : 'oauth.generic' },
      });
      return;
    }

    const result = tokenStorage.syncFromCookies();
    if (result.hasAccessToken) {
      authStore.refreshFromStorage();
      navigate(routes.home, { replace: true });
      return;
    }

    authStore.clear();
    navigate(routes.login, { replace: true, state: { messageKey: 'oauth.missingCookies' } });
  }, [location.search, navigate]);

  return (
    <main className="auth-page">
      <section className="auth-panel compact" aria-live="polite">
        <h1 className="auth-title">{t('token.title')}</h1>
        <p className="auth-description">{t('token.checking')}</p>
      </section>
    </main>
  );
}

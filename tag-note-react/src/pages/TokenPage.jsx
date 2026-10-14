import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { authStore } from '../store/authStore.js';
import { routes } from '../utils/routes.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import '../styles/auth.css';

export default function TokenPage() {
  const { t } = useTranslation('auth');
  const navigate = useNavigate();

  useEffect(() => {
    const result = tokenStorage.syncFromCookies();
    authStore.refreshFromStorage();

    navigate(result.hasPendingToken ? routes.nickname : routes.home, { replace: true });
  }, [navigate]);

  return (
    <main className="auth-page">
      <section className="auth-panel compact" aria-live="polite">
        <h1 className="auth-title">{t('token.title')}</h1>
        <p className="auth-description">{t('token.checking')}</p>
      </section>
    </main>
  );
}

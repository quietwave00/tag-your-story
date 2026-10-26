import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation, useNavigate } from 'react-router-dom';
import { getOAuthAuthorizationUrl } from '../services/authService.js';
import { useAuthStore } from '../store/authStore.js';
import { routes } from '../utils/routes.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import '../styles/auth.css';

export default function LoginPage() {
  const { t } = useTranslation('auth');
  const navigate = useNavigate();
  const location = useLocation();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const message = location.state?.messageKey
    ? t(location.state.messageKey, location.state.messageParams)
    : location.state?.message;

  useEffect(() => {
    if (isAuthenticated) {
      navigate(routes.home, { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const handleGoogleLogin = () => {
    tokenStorage.prepareForLogin();
    window.location.assign(getOAuthAuthorizationUrl('google'));
  };

  return (
    <main className="auth-page">
      <section className="auth-panel" aria-labelledby="login-title">
        <h1 className="auth-title" id="login-title">
          {t('login.title')}
        </h1>
        <button className="auth-primary-button" type="button" onClick={handleGoogleLogin}>
          {t('login.google')}
        </button>
        {message ? <p className="auth-message">{message}</p> : null}
        <p className="auth-brand">#tagnote</p>
      </section>
    </main>
  );
}

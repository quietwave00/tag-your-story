import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import GoogleLoginButton from '../features/auth/GoogleLoginButton.jsx';
import { finishEmailSignIn, getEmailAuthErrorKey, normalizeEmail } from '../features/auth/emailAuth.js';
import { authService, getOAuthAuthorizationUrl } from '../services/authService.js';
import { authStore, useAuthStore } from '../store/authStore.js';
import { routes } from '../utils/routes.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import '../styles/auth.css';

export default function LoginPage() {
  const { t } = useTranslation('auth');
  const navigate = useNavigate();
  const location = useLocation();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorKey, setErrorKey] = useState('');
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

  const handleEmailLogin = async (event) => {
    event.preventDefault();
    if (isSubmitting) return;
    if (!email.trim() || !password) {
      setErrorKey('login.required');
      return;
    }

    setIsSubmitting(true);
    setErrorKey('');
    try {
      tokenStorage.prepareForLogin();
      const result = await authService.loginWithEmail(normalizeEmail(email), password);
      if (!finishEmailSignIn()) {
        setErrorKey('login.missingCookies');
        return;
      }
      authStore.setNickname(result?.nickname);
      navigate(routes.home, { replace: true });
    } catch (error) {
      setErrorKey(getEmailAuthErrorKey(error, 'login'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="auth-page login-page">
      <section className="auth-panel" aria-labelledby="login-title">
        <h1 className="auth-title" id="login-title">
          {t('login.title')}
        </h1>
        <form aria-busy={isSubmitting} className="auth-form" noValidate onSubmit={handleEmailLogin}>
          <label className="auth-field" htmlFor="login-email">
            <span>{t('login.email')}</span>
            <input
              autoComplete="email"
              id="login-email"
              maxLength={255}
              onChange={(event) => { setEmail(event.target.value); setErrorKey(''); }}
              required
              type="email"
              value={email}
            />
          </label>
          <div className="auth-field">
            <label htmlFor="login-password">{t('login.password')}</label>
            <span className="auth-password-field">
              <input
                autoComplete="current-password"
                id="login-password"
                onChange={(event) => { setPassword(event.target.value); setErrorKey(''); }}
                required
                type={showPassword ? 'text' : 'password'}
                value={password}
              />
              <button aria-label={t(showPassword ? 'login.hidePassword' : 'login.showPassword')} onClick={() => setShowPassword((visible) => !visible)} type="button">
                {t(showPassword ? 'login.hide' : 'login.show')}
              </button>
            </span>
          </div>
          {errorKey ? <p className="auth-form-error" role="alert">{t(errorKey)}</p> : null}
          <button className="auth-email-button" disabled={isSubmitting} type="submit">
            {t(isSubmitting ? 'login.submitting' : 'login.emailSubmit')}
          </button>
        </form>
        <div className="auth-divider"><span>{t('login.or')}</span></div>
        <GoogleLoginButton disabled={isSubmitting} onClick={handleGoogleLogin} />
        <p className="auth-switch">{t('login.noAccount')} <Link to={routes.signup}>{t('login.signupLink')}</Link></p>
        {message ? <p className="auth-message">{message}</p> : null}
      </section>
    </main>
  );
}

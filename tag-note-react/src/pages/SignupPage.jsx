import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router-dom';
import GoogleLoginButton from '../features/auth/GoogleLoginButton.jsx';
import { normalizeEmail } from '../features/auth/emailAuth.js';
import { useEmailSignup } from '../features/auth/useEmailSignup.js';
import { getOAuthAuthorizationUrl } from '../services/authService.js';
import { routes } from '../utils/routes.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import '../styles/auth.css';

function timeLabel(seconds) {
  return `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
}

export default function SignupPage() {
  const { t } = useTranslation('auth');
  const navigate = useNavigate();
  const {
    wasAuthenticatedOnEntry, step, email, setEmail, password, setPassword,
    confirmation, setConfirmation, showPassword, setShowPassword,
    code, setCode, remaining, resendRemaining, sentCount, isSubmitting,
    fieldError, setFieldError, errorKey, setErrorKey, nickname,
    handleStart, handleResend, handleConfirm, changeEmail,
  } = useEmailSignup();

  useEffect(() => {
    if (wasAuthenticatedOnEntry) navigate(routes.home, { replace: true });
  }, [navigate, wasAuthenticatedOnEntry]);

  const handleGoogleLogin = () => {
    tokenStorage.prepareForLogin();
    window.location.assign(getOAuthAuthorizationUrl('google'));
  };

  if (wasAuthenticatedOnEntry) return null;

  return (
    <main className="auth-page signup-page">
      <section className="auth-panel signup-panel" aria-labelledby="signup-title">
        <h1 className="auth-title" id="signup-title">{t(step === 'verify' ? 'signup.verifyTitle' : step === 'success' ? 'signup.successTitle' : 'signup.title')}</h1>

        {step === 'form' ? (
          <>
            <form aria-busy={isSubmitting} className="auth-form" noValidate onSubmit={handleStart}>
              <label className="auth-field" htmlFor="signup-email">
                <span>{t('login.email')}</span>
                <input aria-describedby={fieldError === 'signup.emailInvalid' ? 'signup-email-error' : undefined} aria-invalid={fieldError === 'signup.emailInvalid'} autoComplete="email" id="signup-email" maxLength={255} onChange={(event) => { setEmail(event.target.value); setFieldError(''); }} required type="email" value={email} />
              </label>
              {fieldError === 'signup.emailInvalid' ? <p className="auth-field-error" id="signup-email-error" role="alert">{t(fieldError)}</p> : null}
              <div className="auth-field">
                <label htmlFor="signup-password">{t('login.password')}</label>
                <span className="auth-password-field">
                  <input aria-describedby={fieldError && fieldError !== 'signup.emailInvalid' ? 'signup-password-error' : undefined} aria-invalid={Boolean(fieldError && fieldError !== 'signup.emailInvalid')} autoComplete="new-password" id="signup-password" onChange={(event) => { setPassword(event.target.value); setFieldError(''); }} required type={showPassword ? 'text' : 'password'} value={password} />
                  <button aria-label={t(showPassword ? 'login.hidePassword' : 'login.showPassword')} onClick={() => setShowPassword((visible) => !visible)} type="button">{t(showPassword ? 'login.hide' : 'login.show')}</button>
                </span>
              </div>
              <ul className="auth-password-rules">
                <li>{t('signup.ruleLength')}</li>
                <li>{t('signup.ruleLettersNumbers')}</li>
                <li>{t('signup.ruleBytes')}</li>
                <li>{t('signup.ruleSpecial')}</li>
              </ul>
              <label className="auth-field" htmlFor="signup-confirmation">
                <span>{t('signup.passwordConfirm')}</span>
                <input aria-describedby={fieldError === 'signup.passwordMismatch' ? 'signup-password-error' : undefined} aria-invalid={fieldError === 'signup.passwordMismatch'} autoComplete="new-password" id="signup-confirmation" onChange={(event) => { setConfirmation(event.target.value); setFieldError(''); }} required type={showPassword ? 'text' : 'password'} value={confirmation} />
              </label>
              {fieldError && fieldError !== 'signup.emailInvalid' ? <p className="auth-field-error" id="signup-password-error" role="alert">{t(fieldError)}</p> : null}
              {errorKey ? <p className="auth-form-error" role="alert">{t(errorKey)}</p> : null}
              <button className="auth-email-button" disabled={isSubmitting} type="submit">{t(isSubmitting ? 'signup.sending' : 'signup.sendCode')}</button>
            </form>
            <div className="auth-divider"><span>{t('login.or')}</span></div>
            <GoogleLoginButton disabled={isSubmitting} onClick={handleGoogleLogin} />
            <p className="auth-switch"><Link to={routes.login}>{t('signup.backToLogin')}</Link></p>
          </>
        ) : null}

        {step === 'verify' ? (
          <>
            <p className="auth-lead">{t('signup.sentTo')} <strong>{normalizeEmail(email)}</strong></p>
            <form aria-busy={isSubmitting} className="auth-form" noValidate onSubmit={handleConfirm}>
              <label className="auth-field" htmlFor="signup-code"><span>{t('signup.code')}</span>
                <input autoComplete="one-time-code" id="signup-code" inputMode="numeric" onChange={(event) => { setCode(event.target.value.replace(/\D/g, '').slice(0, 6)); setErrorKey(''); }} pattern="[0-9]*" required type="text" value={code} />
              </label>
              <p className="auth-hint">{remaining > 0 ? t('signup.timeLeft', { time: timeLabel(remaining) }) : t('signup.expired')}</p>
              {errorKey ? <p className="auth-form-error" role="alert">{t(errorKey)}</p> : null}
              <button className="auth-email-button" disabled={isSubmitting || code.length !== 6 || remaining === 0} type="submit">{t(isSubmitting ? 'signup.confirming' : 'signup.confirm')}</button>
            </form>
            <button className="auth-text-button" disabled={isSubmitting || remaining === 0 || resendRemaining > 0 || sentCount >= 5} onClick={handleResend} type="button">
              {resendRemaining > 0 ? t('signup.resendWait', { seconds: resendRemaining }) : t('signup.resend')}
            </button>
            {sentCount >= 5 ? <p className="auth-hint">{t('signup.resendLimit')}</p> : null}
            <button className="auth-text-button" disabled={isSubmitting} onClick={changeEmail} type="button">{t('signup.changeEmail')}</button>
          </>
        ) : null}

        {step === 'success' ? (
          <>
            {nickname ? <p className="auth-lead">{t('signup.welcome', { nickname })}</p> : null}
            <button className="auth-email-button" onClick={() => navigate(routes.home, { replace: true })} type="button">{t('signup.goHome')}</button>
          </>
        ) : null}
      </section>
    </main>
  );
}

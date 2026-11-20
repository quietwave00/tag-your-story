import { useEffect, useState } from 'react';
import { authService } from '../../services/authService.js';
import { authStore, useAuthStore } from '../../store/authStore.js';
import { tokenStorage } from '../../utils/tokenStorage.js';
import { finishEmailSignIn, getEmailAuthErrorKey, isValidEmail, normalizeEmail, validateSignupPassword } from './emailAuth.js';

export function useEmailSignup() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const [wasAuthenticatedOnEntry] = useState(isAuthenticated);
  const [step, setStep] = useState('form');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [challengeId, setChallengeId] = useState('');
  const [code, setCode] = useState('');
  const [expiresAt, setExpiresAt] = useState(0);
  const [resendAt, setResendAt] = useState(0);
  const [sentCount, setSentCount] = useState(0);
  const [now, setNow] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [fieldError, setFieldError] = useState('');
  const [errorKey, setErrorKey] = useState('');
  const [nickname, setNickname] = useState('');

  useEffect(() => {
    if (step !== 'verify') return undefined;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [step]);

  const remaining = Math.max(0, Math.ceil((expiresAt - now) / 1000));
  const resendRemaining = Math.max(0, Math.ceil((resendAt - now) / 1000));

  const handleStart = async (event) => {
    event.preventDefault();
    if (isSubmitting) return;
    setFieldError('');
    setErrorKey('');
    if (!isValidEmail(email)) {
      setFieldError('signup.emailInvalid');
      return;
    }
    const passwordError = validateSignupPassword(password, confirmation);
    if (passwordError) {
      setFieldError(passwordError);
      return;
    }

    setIsSubmitting(true);
    try {
      const result = await authService.startEmailSignup(normalizeEmail(email), password, confirmation);
      if (!result?.challengeId) {
        setErrorKey('signup.unavailable');
        return;
      }
      const startedAt = Date.now();
      setNow(startedAt);
      setExpiresAt(startedAt + 10 * 60 * 1000);
      setResendAt(startedAt + 60 * 1000);
      setSentCount(1);
      setChallengeId(result.challengeId);
      setPassword('');
      setConfirmation('');
      setStep('verify');
    } catch (error) {
      setErrorKey(getEmailAuthErrorKey(error, 'signup'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResend = async () => {
    if (isSubmitting || remaining === 0 || resendRemaining > 0 || sentCount >= 5) return;
    setIsSubmitting(true);
    setErrorKey('');
    try {
      await authService.resendEmailSignup(challengeId);
      const sentAt = Date.now();
      setNow(sentAt);
      setResendAt(sentAt + 60 * 1000);
      setSentCount((count) => count + 1);
      setCode('');
    } catch (error) {
      setErrorKey(getEmailAuthErrorKey(error, 'signup'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleConfirm = async (event) => {
    event.preventDefault();
    if (isSubmitting || code.length !== 6 || remaining === 0) return;
    setIsSubmitting(true);
    setErrorKey('');
    try {
      tokenStorage.prepareForLogin();
      const result = await authService.confirmEmailSignup(challengeId, code);
      if (!finishEmailSignIn()) {
        setErrorKey('signup.missingCookies');
        return;
      }
      authStore.setNickname(result?.nickname);
      setNickname(result?.nickname ?? '');
      setStep('success');
    } catch (error) {
      const key = getEmailAuthErrorKey(error, 'signup');
      setErrorKey(key);
      if (key === 'signup.challengeInvalid') setExpiresAt(Date.now());
    } finally {
      setIsSubmitting(false);
    }
  };

  const changeEmail = () => {
    if (isSubmitting) return;
    setStep('form');
    setChallengeId('');
    setCode('');
    setErrorKey('');
  };

  return {
    wasAuthenticatedOnEntry, step, email, setEmail, password, setPassword,
    confirmation, setConfirmation, showPassword, setShowPassword,
    code, setCode, remaining, resendRemaining, sentCount, isSubmitting,
    fieldError, setFieldError, errorKey, setErrorKey, nickname,
    handleStart, handleResend, handleConfirm, changeEmail,
  };
}

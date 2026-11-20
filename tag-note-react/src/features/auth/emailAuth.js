import { authStore } from '../../store/authStore.js';
import { tokenStorage } from '../../utils/tokenStorage.js';

const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export function normalizeEmail(email) {
  return email.trim().toLowerCase();
}

export function isValidEmail(email) {
  const normalized = normalizeEmail(email);
  return normalized.length <= 255 && EMAIL_PATTERN.test(normalized);
}

export function validateSignupPassword(password, confirmation) {
  if (password.length < 8 || new TextEncoder().encode(password).length > 72
    || !/[A-Za-z]/.test(password) || !/\d/.test(password)) {
    return 'signup.passwordRuleError';
  }
  if (password !== confirmation) return 'signup.passwordMismatch';
  return '';
}

export function finishEmailSignIn() {
  const result = tokenStorage.syncFromCookies();
  if (!result.hasAccessToken) return false;
  authStore.refreshFromStorage();
  return true;
}

const LOGIN_ERRORS = {
  LOGIN_INVALID_CREDENTIALS: 'login.invalidCredentials',
  GOOGLE_ACCOUNT_EXISTS: 'login.googleAccount',
  USER_INACTIVE: 'login.inactive',
  LOGIN_RATE_LIMITED: 'login.rateLimited',
  LOGIN_UNAVAILABLE: 'login.unavailable',
};

const SIGNUP_ERRORS = {
  SIGNUP_INPUT_INVALID: 'signup.inputInvalid',
  LOCAL_ACCOUNT_EXISTS: 'signup.localAccount',
  GOOGLE_ACCOUNT_EXISTS: 'signup.googleAccount',
  SIGNUP_RATE_LIMITED: 'signup.rateLimited',
  SIGNUP_MAIL_UNAVAILABLE: 'signup.mailUnavailable',
  SIGNUP_CHALLENGE_UNAVAILABLE: 'signup.unavailable',
  SIGNUP_CHALLENGE_INVALID: 'signup.challengeInvalid',
  SIGNUP_CODE_INVALID: 'signup.codeInvalid',
  SIGNUP_CONFIRM_BUSY: 'signup.confirmBusy',
  SIGNUP_CONFLICT: 'signup.conflict',
  USER_INACTIVE: 'signup.unavailable',
};

export function getEmailAuthErrorKey(error, flow) {
  const errors = flow === 'login' ? LOGIN_ERRORS : SIGNUP_ERRORS;
  return errors[error?.exceptionCode] ?? (flow === 'login' ? 'login.unavailable' : 'signup.unavailable');
}

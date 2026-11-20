import { describe, expect, it } from 'vitest';
import { getEmailAuthErrorKey, isValidEmail, validateSignupPassword } from './emailAuth.js';

describe('LOCAL account input and error mapping', () => {
  it('validates the server email shape without alias normalization', () => {
    expect(isValidEmail(' Name+tag@Gmail.com ')).toBe(true);
    expect(isValidEmail('not-an-email')).toBe(false);
    expect(isValidEmail(`${'a'.repeat(251)}@x.io`)).toBe(false);
  });

  it('enforces password length, ASCII letters, digits, UTF-8 bytes, and confirmation', () => {
    expect(validateSignupPassword('password1', 'password1')).toBe('');
    expect(validateSignupPassword('password', 'password')).toBe('signup.passwordRuleError');
    expect(validateSignupPassword(`${'a'.repeat(70)}1한`, `${'a'.repeat(70)}1한`)).toBe('signup.passwordRuleError');
    expect(validateSignupPassword('password1', 'password2')).toBe('signup.passwordMismatch');
  });

  it('keeps invalid LOCAL credentials generic and suggests Google for a Google account', () => {
    expect(getEmailAuthErrorKey({ exceptionCode: 'LOGIN_INVALID_CREDENTIALS' }, 'login')).toBe('login.invalidCredentials');
    expect(getEmailAuthErrorKey({ exceptionCode: 'GOOGLE_ACCOUNT_EXISTS' }, 'login')).toBe('login.googleAccount');
  });
});

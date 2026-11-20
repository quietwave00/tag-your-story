import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import i18n from '../i18n/index.js';
import { authService } from '../services/authService.js';
import { authStore } from '../store/authStore.js';
import LoginPage from './LoginPage.jsx';
import SignupPage from './SignupPage.jsx';

function renderAuth() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/" element={<p>Home destination</p>} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(async () => {
  authStore.clear();
  localStorage.clear();
  await i18n.changeLanguage('en');
  document.cookie = 'Authorization=; max-age=0; path=/';
  document.cookie = 'RefreshToken=; max-age=0; path=/';
});

afterEach(() => {
  cleanup();
  vi.useRealTimers();
  vi.restoreAllMocks();
  authStore.clear();
});

describe('email authentication', () => {
  it('keeps unknown email and wrong password under the same login error', async () => {
    vi.spyOn(authService, 'loginWithEmail').mockRejectedValue({ exceptionCode: 'LOGIN_INVALID_CREDENTIALS' });
    renderAuth();

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: ' USER@EXAMPLE.COM ' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'wrong-password' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign in with email' }));

    expect(await screen.findByText('Email or password is incorrect.')).toBeInTheDocument();
    expect(authService.loginWithEmail).toHaveBeenCalledWith('user@example.com', 'wrong-password');
  });

  it('synchronizes token cookies after email login', async () => {
    vi.spyOn(authService, 'loginWithEmail').mockImplementation(async () => {
      document.cookie = 'Authorization=Bearer%20access-token; path=/';
      document.cookie = 'RefreshToken=Bearer%20refresh-token; path=/';
      return { userId: 1, nickname: 'Listener' };
    });
    renderAuth();

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign in with email' }));

    expect(await screen.findByText('Home destination')).toBeInTheDocument();
    expect(authStore.getState()).toMatchObject({ accessToken: 'access-token', isAuthenticated: true });
    expect(authStore.getState().nickname).toBe('Listener');
  });

  it('does not claim an email login succeeded when session cookies are missing', async () => {
    vi.spyOn(authService, 'loginWithEmail').mockResolvedValue({ userId: 1, nickname: 'Listener' });
    renderAuth();

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Sign in with email' }));

    expect(await screen.findByText(/browser did not receive the session/i)).toBeInTheDocument();
    expect(authStore.getState().isAuthenticated).toBe(false);
  });

  it('starts email sign-up, verifies a code, and shows the server-generated nickname', async () => {
    vi.spyOn(authService, 'startEmailSignup').mockResolvedValue({ challengeId: 'challenge-1' });
    vi.spyOn(authService, 'confirmEmailSignup').mockImplementation(async () => {
      document.cookie = 'Authorization=Bearer%20access-token; path=/';
      document.cookie = 'RefreshToken=Bearer%20refresh-token; path=/';
      return { userId: 1, nickname: '푸른여우4821' };
    });
    renderAuth();
    fireEvent.click(screen.getByRole('link', { name: 'Create an account' }));

    fireEvent.change(screen.getByLabelText('Email'), { target: { value: ' USER@EXAMPLE.COM ' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password1' } });
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: 'password1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send verification email' }));

    expect(await screen.findByText(/We sent a six-digit code to/)).toBeInTheDocument();
    expect(authService.startEmailSignup).toHaveBeenCalledWith('user@example.com', 'password1', 'password1');
    fireEvent.change(screen.getByLabelText('Verification code'), { target: { value: '12 34-56' } });
    fireEvent.click(screen.getByRole('button', { name: 'Complete sign-up' }));

    expect(await screen.findByText('Welcome, 푸른여우4821.')).toBeInTheDocument();
    expect(authService.confirmEmailSignup).toHaveBeenCalledWith('challenge-1', '123456');
    expect(authStore.getState().isAuthenticated).toBe(true);
    expect(authStore.getState().nickname).toBe('푸른여우4821');
  });

  it('keeps invalid passwords on the first step without calling the server', async () => {
    vi.spyOn(authService, 'startEmailSignup');
    renderAuth();
    fireEvent.click(screen.getByRole('link', { name: 'Create an account' }));
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'short' } });
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: 'short' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send verification email' }));

    expect(screen.getByText('Your password does not meet the requirements above.')).toBeInTheDocument();
    await waitFor(() => expect(authService.startEmailSignup).not.toHaveBeenCalled());
  });

  it('enables code resend after the 60-second cooldown', async () => {
    vi.spyOn(authService, 'startEmailSignup').mockResolvedValue({ challengeId: 'challenge-1' });
    vi.spyOn(authService, 'resendEmailSignup').mockResolvedValue(undefined);
    renderAuth();
    fireEvent.click(screen.getByRole('link', { name: 'Create an account' }));
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'user@example.com' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'password1' } });
    fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: 'password1' } });

    vi.useFakeTimers();
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Send verification email' })); });
    expect(screen.getByRole('button', { name: /Resend code in/ })).toBeDisabled();

    act(() => vi.advanceTimersByTime(60_000));
    await act(async () => { fireEvent.click(screen.getByRole('button', { name: 'Resend code' })); });
    expect(authService.resendEmailSignup).toHaveBeenCalledWith('challenge-1');
  });
});

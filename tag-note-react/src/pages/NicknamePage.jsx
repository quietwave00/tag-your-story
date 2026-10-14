import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/authService.js';
import { authStore, useAuthStore } from '../store/authStore.js';
import { routes } from '../utils/routes.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import '../styles/auth.css';

function validateNickname(nickname) {
  if (!nickname) {
    return 'nickname.validation.required';
  }

  if (/\s/.test(nickname)) {
    return 'nickname.validation.noSpaces';
  }

  if (nickname.length > 7) {
    return 'nickname.validation.maxLength';
  }

  return '';
}

export default function NicknamePage() {
  const { t } = useTranslation('auth');
  const navigate = useNavigate();
  const isPendingUser = useAuthStore((state) => state.isPendingUser);
  const [nickname, setNickname] = useState('');
  const [messageKey, setMessageKey] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    tokenStorage.syncFromCookies();
    authStore.refreshFromStorage();
  }, []);

  useEffect(() => {
    if (!isPendingUser) {
      navigate(routes.home, { replace: true });
    }
  }, [isPendingUser, navigate]);

  const handleSubmit = async (event) => {
    event.preventDefault();

    const trimmedNickname = nickname.trim();
    const validationMessageKey = validateNickname(trimmedNickname);

    if (validationMessageKey) {
      setMessageKey(validationMessageKey);
      if (/\s/.test(trimmedNickname) || trimmedNickname.length > 7) {
        setNickname('');
      }
      return;
    }

    const shouldRegister = window.confirm(t('nickname.confirm', { nickname: trimmedNickname }));

    if (!shouldRegister) {
      return;
    }

    setIsSubmitting(true);

    try {
      await authService.updateNickname(trimmedNickname);
      authStore.clearPending();
      navigate(routes.login, {
        replace: true,
        state: {
          messageKey: 'nickname.registered',
          messageParams: { nickname: trimmedNickname },
        },
      });
    } catch {
      setMessageKey('nickname.requestFailed');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="auth-page">
      <form className="auth-panel" onSubmit={handleSubmit}>
        <h1 className="auth-title">{t('nickname.title')}</h1>
        <p className="auth-description">{t('nickname.description')}</p>
        {messageKey ? <p className="auth-message">{t(messageKey)}</p> : <p className="auth-message placeholder"> </p>}
        <input
          className="nickname-input"
          aria-label={t('nickname.inputLabel')}
          onChange={(event) => {
            setNickname(event.target.value);
            setMessageKey('');
          }}
          type="text"
          value={nickname}
        />
        <button className="nickname-button" disabled={isSubmitting} type="submit">
          {t('nickname.save')}
        </button>
      </form>
    </main>
  );
}

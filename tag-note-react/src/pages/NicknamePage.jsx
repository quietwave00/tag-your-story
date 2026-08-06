import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/authService.js';
import { authStore, useAuthStore } from '../store/authStore.js';
import { routes } from '../utils/routes.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import '../styles/auth.css';

function validateNickname(nickname) {
  if (!nickname) {
    return '사용하실 닉네임을 입력해 주세요.';
  }

  if (/\s/.test(nickname)) {
    return '공백을 제거해 주세요.';
  }

  if (nickname.length > 7) {
    return '7자를 초과할 수 없습니다.';
  }

  return '';
}

export default function NicknamePage() {
  const navigate = useNavigate();
  const isPendingUser = useAuthStore((state) => state.isPendingUser);
  const [nickname, setNickname] = useState('');
  const [message, setMessage] = useState('');
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
    const validationMessage = validateNickname(trimmedNickname);

    if (validationMessage) {
      setMessage(validationMessage);
      if (/\s/.test(trimmedNickname) || trimmedNickname.length > 7) {
        setNickname('');
      }
      return;
    }

    const shouldRegister = window.confirm(`${trimmedNickname}으로 가입하시겠습니까?`);

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
          message: `${trimmedNickname}님, 회원가입이 정상적으로 완료되었습니다. 다시 로그인 해주세요.`,
        },
      });
    } catch {
      setMessage('닉네임 설정 요청을 처리하지 못했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="auth-page">
      <form className="auth-panel" onSubmit={handleSubmit}>
        <h1 className="auth-title">SET NICKNAME</h1>
        <p className="auth-description">공백 없이 7자까지 사용할 수 있습니다.</p>
        {message ? <p className="auth-message">{message}</p> : <p className="auth-message placeholder"> </p>}
        <input
          className="nickname-input"
          onChange={(event) => {
            setNickname(event.target.value);
            setMessage('');
          }}
          type="text"
          value={nickname}
        />
        <button className="nickname-button" disabled={isSubmitting} type="submit">
          SAVE ID
        </button>
      </form>
    </main>
  );
}

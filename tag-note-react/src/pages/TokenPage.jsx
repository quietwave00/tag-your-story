import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authStore } from '../store/authStore.js';
import { routes } from '../utils/routes.js';
import { tokenStorage } from '../utils/tokenStorage.js';
import '../styles/auth.css';

export default function TokenPage() {
  const navigate = useNavigate();

  useEffect(() => {
    const result = tokenStorage.syncFromCookies();
    authStore.refreshFromStorage();

    navigate(result.hasPendingToken ? routes.nickname : routes.home, { replace: true });
  }, [navigate]);

  return (
    <main className="auth-page">
      <section className="auth-panel compact" aria-live="polite">
        <h1 className="auth-title">SYNC TOKEN</h1>
        <p className="auth-description">로그인 정보를 확인하고 있습니다 █</p>
      </section>
    </main>
  );
}

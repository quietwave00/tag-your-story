import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { accountService } from '../services/accountService.js';
import { authService } from '../services/authService.js';
import { likeService } from '../services/likeService.js';
import { authStore, useAuthStore } from '../store/authStore.js';
import { buildRoute, routes } from '../utils/routes.js';
import '../styles/myPage.css';

const PAGE_SIZE = 8;
const SECTIONS = ['tags', 'likes', 'settings', 'account'];

export default function MyPage() {
  const { t } = useTranslation('myPage');
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const displayNickname = useAuthStore((state) => state.nickname);
  const [section, setSection] = useState('tags');
  const [likedPage, setLikedPage] = useState(1);
  const [likedState, setLikedState] = useState({ items: [], totalCount: 0, loading: false, error: false });
  const [nickname, setNickname] = useState('');
  const [nicknameState, setNicknameState] = useState({ loading: false, error: '', saved: '' });
  const [accountState, setAccountState] = useState({ loading: false, error: '' });
  const [confirmWithdrawal, setConfirmWithdrawal] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) navigate(routes.login, { replace: true });
  }, [isAuthenticated, navigate]);

  useEffect(() => {
    if (!isAuthenticated || section !== 'likes') return undefined;
    let current = true;
    likeService.getMyLikedBoards(likedPage)
      .then((response) => {
        if (current) setLikedState({ items: response?.items ?? [], totalCount: response?.totalCount ?? 0, loading: false, error: false });
      })
      .catch(() => {
        if (current) setLikedState({ items: [], totalCount: 0, loading: false, error: true });
      });
    return () => { current = false; };
  }, [isAuthenticated, section, likedPage]);

  if (!isAuthenticated) return null;

  const selectSection = (next) => {
    if (next === section) return;
    if (next === 'likes') setLikedState((previous) => ({ ...previous, loading: true, error: false }));
    setSection(next);
    setConfirmWithdrawal(false);
  };

  const saveNickname = async (event) => {
    event.preventDefault();
    const next = nickname;
    if (!next || next.length > 20 || /\s/.test(next)) {
      setNicknameState({ loading: false, error: 'nicknameInvalid', saved: '' });
      return;
    }
    setNicknameState({ loading: true, error: '', saved: '' });
    try {
      const response = await accountService.changeNickname(next);
      authStore.setNickname(response.nickname);
      setNicknameState({ loading: false, error: '', saved: response.nickname });
      setNickname('');
    } catch (error) {
      setNicknameState({ loading: false, error: error?.exceptionCode === 'NICKNAME_ALREADY_EXISTS' ? 'nicknameExists' : 'nicknameFailed', saved: '' });
    }
  };

  const logout = async () => {
    if (accountState.loading) return;
    setAccountState({ loading: true, error: '' });
    let failed = false;
    try {
      await authService.logout();
    } catch {
      failed = true;
    } finally {
      authStore.clear();
      navigate(failed ? routes.login : routes.home, {
        state: failed ? { messageKey: 'oauth.logoutFailed' } : undefined,
      });
    }
  };

  const withdraw = async () => {
    if (!confirmWithdrawal || accountState.loading) return;
    setAccountState({ loading: true, error: '' });
    try {
      await accountService.withdraw();
      authStore.clear();
      navigate(routes.home, { replace: true });
    } catch {
      setAccountState({ loading: false, error: 'withdrawFailed' });
    }
  };

  const totalPages = Math.ceil(likedState.totalCount / PAGE_SIZE);

  return (
    <section className="my-page" aria-labelledby="my-page-title">
      <header className="my-page-header">
        <h1 id="my-page-title">{displayNickname || t('accountFallback')}</h1>
      </header>

      <div className="my-page-layout">
        <nav aria-label={t('sectionNavigation')} className="my-page-nav">
          {SECTIONS.map((item) => (
            <button aria-current={section === item ? 'page' : undefined} className="my-page-nav-item" key={item} onClick={() => selectSection(item)} type="button">
              <span>{t(`sections.${item}`)}</span>
            </button>
          ))}
        </nav>

        <div className="my-page-panel">
          {section === 'tags' ? (
            <section aria-labelledby="my-tags-title">
              <h2 id="my-tags-title">{t('sections.tags')}</h2>
              <p className="my-page-description">{t('tags.description')}</p>
              <div className="my-page-empty" role="status">
                <span aria-hidden="true" className="my-page-empty-mark">#</span>
                <p>{t('tags.unavailable')}</p>
              </div>
            </section>
          ) : null}

          {section === 'likes' ? (
            <section aria-labelledby="my-likes-title">
              <h2 id="my-likes-title">{t('sections.likes')}</h2>
              <p className="my-page-description">{t('likes.description')}</p>
              {likedState.loading ? (
                <div className="my-page-liked-list my-page-liked-skeleton" role="status">
                  <span className="visually-hidden">{t('likes.loading')}</span>
                  {[0, 1, 2].map((index) => (
                    <div aria-hidden="true" className="my-page-liked-item" key={index}><span className="skeleton-block" /><span className="skeleton-block" /></div>
                  ))}
                </div>
              ) : null}
              {!likedState.loading && likedState.error ? <p className="my-page-message" role="alert">{t('likes.failed')}</p> : null}
              {!likedState.loading && !likedState.error && likedState.items.length === 0 ? <p className="my-page-empty">{t('likes.empty')}</p> : null}
              {!likedState.loading && !likedState.error && likedState.items.length > 0 ? (
                <div className="my-page-liked-list">
                  {likedState.items.map((item) => (
                    <Link className="my-page-liked-item" key={item.boardId} to={buildRoute.boardDetail(item.boardId)}>
                      <span className="my-page-liked-author">{item.author}</span>
                      <span className="my-page-liked-content">{item.content}</span>
                    </Link>
                  ))}
                </div>
              ) : null}
              {!likedState.loading && !likedState.error && totalPages > 1 ? (
                <div className="my-page-pagination">
                  <button disabled={likedPage === 1} onClick={() => { setLikedState((previous) => ({ ...previous, loading: true })); setLikedPage(likedPage - 1); }} type="button">{t('likes.previous')}</button>
                  <span>{likedPage} / {totalPages}</span>
                  <button disabled={likedPage >= totalPages} onClick={() => { setLikedState((previous) => ({ ...previous, loading: true })); setLikedPage(likedPage + 1); }} type="button">{t('likes.next')}</button>
                </div>
              ) : null}
            </section>
          ) : null}

          {section === 'settings' ? (
            <section aria-labelledby="my-settings-title">
              <h2 id="my-settings-title">{t('sections.settings')}</h2>
              <p className="my-page-description">{t('settings.description')}</p>
              <form className="my-page-form" onSubmit={saveNickname}>
                <label htmlFor="my-page-nickname">{t('settings.nickname')}</label>
                <input autoComplete="nickname" id="my-page-nickname" maxLength={20} onChange={(event) => setNickname(event.target.value)} value={nickname} />
                <p className="my-page-hint">{t('settings.hint')}</p>
                {nicknameState.error ? <p className="my-page-error" role="alert">{t(`settings.${nicknameState.error}`)}</p> : null}
                {nicknameState.saved ? <p className="my-page-success" role="status">{t('settings.saved', { nickname: nicknameState.saved })}</p> : null}
                <button className="my-page-primary" disabled={nicknameState.loading || !nickname.trim()} type="submit">{nicknameState.loading ? t('settings.saving') : t('settings.save')}</button>
              </form>
            </section>
          ) : null}

          {section === 'account' ? (
            <section aria-labelledby="my-account-title">
              <h2 id="my-account-title">{t('sections.account')}</h2>
              <p className="my-page-description">{t('account.description')}</p>
              <div className="my-page-account-action">
                <div><h3>{t('account.logout')}</h3><p>{t('account.logoutDescription')}</p></div>
                <button disabled={accountState.loading} onClick={logout} type="button">{t('account.logout')}</button>
              </div>
              <div className="my-page-account-action">
                <div><h3>{t('account.withdraw')}</h3><p>{t('account.withdrawDescription')}</p></div>
                <button disabled={accountState.loading} onClick={() => setConfirmWithdrawal(true)} type="button">{t('account.withdraw')}</button>
              </div>
              {confirmWithdrawal ? (
                <div className="my-page-withdraw-confirm" role="group" aria-label={t('account.confirmTitle')}>
                  <h3>{t('account.confirmTitle')}</h3>
                  <p>{t('account.confirmDescription')}</p>
                  <div>
                    <button disabled={accountState.loading} onClick={() => setConfirmWithdrawal(false)} type="button">{t('account.cancel')}</button>
                    <button disabled={accountState.loading} onClick={withdraw} type="button">{accountState.loading ? t('account.processing') : t('account.confirm')}</button>
                  </div>
                </div>
              ) : null}
              {accountState.error ? <p className="my-page-error" role="alert">{t(`account.${accountState.error}`)}</p> : null}
            </section>
          ) : null}
        </div>
      </div>
    </section>
  );
}

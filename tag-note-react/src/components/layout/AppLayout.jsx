import { useEffect, useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import NotificationCenter from '../../features/notifications/NotificationCenter.jsx';
import UserAvatar from '../common/UserAvatar.jsx';
import { useAuthStore } from '../../store/authStore.js';
import { routes } from '../../utils/routes.js';
import logoUrl from '../../../assets/tagnote-logo-transparent.png';

const navItems = [
  { to: routes.genres, labelKey: 'navigation.genres' },
  { to: routes.tracks, labelKey: 'navigation.dig' },
];

export default function AppLayout() {
  const { t } = useTranslation('common');
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const nickname = useAuthStore((state) => state.nickname);
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const updateScroll = () => setIsScrolled(window.scrollY > 24);
    updateScroll();
    window.addEventListener('scroll', updateScroll, { passive: true });
    return () => window.removeEventListener('scroll', updateScroll);
  }, []);

  return (
    <div className={isScrolled ? 'app-shell app-shell--scrolled' : 'app-shell'}>
      <header className="app-header">
        <NavLink
          aria-label={t('navigation.brandHome')}
          className="brand image-brand"
          to={routes.home}
        >
          <img className="brand-logo" src={logoUrl} alt="" />
        </NavLink>
        <nav className="app-nav" aria-label={t('navigation.primary')}>
          <div className="app-nav-primary">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
                to={item.to}
              >
                {t(item.labelKey)}
              </NavLink>
            ))}
          </div>
          <div className="app-nav-actions">
            <NotificationCenter />
            {isAuthenticated ? (
              <NavLink
                className={({ isActive }) => `nav-link nav-account${isActive ? ' active' : ''}`}
                to={routes.myPage}
              >
                <UserAvatar name={nickname || t('navigation.accountFallback')} />
                <span>{nickname || t('navigation.accountFallback')}</span>
              </NavLink>
            ) : (
              <NavLink
                className={({ isActive }) => `nav-link nav-login${isActive ? ' active' : ''}`}
                to={routes.login}
              >
                {t('navigation.login')}
              </NavLink>
            )}
          </div>
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
      <footer className="app-footer">
        <div className="app-footer-inner">
          <NavLink aria-label={t('navigation.brandHome')} className="app-footer-brand" to={routes.home}>
            <img alt="" className="app-footer-logo" src={logoUrl} />
          </NavLink>
          <div className="app-footer-meta">
            <nav aria-label={t('footer.navigation')} className="app-footer-nav">
              <NavLink className="app-footer-link" to={routes.contact}>{t('footer.contactMe')}</NavLink>
            </nav>
            <small className="app-footer-copyright">© {new Date().getFullYear()}</small>
          </div>
        </div>
      </footer>
    </div>
  );
}

import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import LanguageSwitcher from '../common/LanguageSwitcher.jsx';
import NotificationCenter from '../../features/notifications/NotificationCenter.jsx';
import { authService } from '../../services/authService.js';
import { authStore, useAuthStore } from '../../store/authStore.js';
import { routes } from '../../utils/routes.js';
import logoUrl from '../../../assets/tagnote-logo-transparent.png';

const navItems = [
  { to: routes.home, labelKey: 'navigation.log' },
  { to: routes.tracks, labelKey: 'navigation.dig' },
  { to: routes.contact, labelKey: 'navigation.contact' },
];

export default function AppLayout() {
  const { t } = useTranslation('common');
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  const handleLogout = async () => {
    try {
      await authService.logout();
    } finally {
      authStore.clear();
      navigate(routes.home);
    }
  };

  return (
    <div className="app-shell">
      <header className="app-header">
        <NavLink
          aria-label={t('navigation.brandHome')}
          className="brand image-brand"
          to={routes.home}
        >
          <img className="brand-logo" src={logoUrl} alt="" />
        </NavLink>
        <nav className="app-nav" aria-label={t('navigation.primary')}>
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              to={item.to}
            >
              {t(item.labelKey)}
            </NavLink>
          ))}
          <NotificationCenter />
          {isAuthenticated ? (
            <button className="nav-action" onClick={handleLogout} type="button">
              {t('navigation.exit')}
            </button>
          ) : (
            <NavLink
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
              to={routes.login}
            >
              {t('navigation.myTags')}
            </NavLink>
          )}
          <LanguageSwitcher />
        </nav>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}

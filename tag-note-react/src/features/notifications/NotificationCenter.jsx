import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { notificationService } from '../../services/notificationService.js';
import { notificationStore, useNotificationStore } from '../../store/notificationStore.js';
import { useAuthStore } from '../../store/authStore.js';
import { buildRoute } from '../../utils/routes.js';
import '../../styles/notificationCenter.css';

const PAGE_SIZE = 5;
const TOAST_VISIBLE_MS = 5000;

function getPublisherName(notification) {
  return notification?.pubNickname ?? notification?.publisher?.nickname ?? notification?.nickname ?? '';
}

function getNotificationMessage(notification, t) {
  const publisherName = getPublisherName(notification);

  if (notification?.type === 'COMMENT') {
    return t('notifications.comment', { publisher: publisherName });
  }

  if (notification?.type === 'LIKE') {
    return t('notifications.like', { publisher: publisherName });
  }

  return t('notifications.generic');
}

function parseNotificationEvent(event) {
  try {
    return JSON.parse(event.data);
  } catch {
    return null;
  }
}

export default function NotificationCenter() {
  const { t } = useTranslation(['community', 'common']);
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const unreadCount = useNotificationStore((state) => state.unreadCount);
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [toasts, setToasts] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [hasError, setHasError] = useState(false);
  const rootRef = useRef(null);
  const openRef = useRef(false);
  const pageRef = useRef(0);
  const seenRef = useRef(new Set());

  const endPage = useMemo(() => {
    const calculatedEndPage = Math.ceil(totalCount / PAGE_SIZE) - 1;

    return calculatedEndPage < 0 ? 0 : calculatedEndPage;
  }, [totalCount]);

  useEffect(() => {
    if (!isAuthenticated) {
      notificationStore.setSubscribed(false);
      notificationStore.setUnreadCount(0);
      setNotifications([]);
      setToasts([]);
      setHasError(false);
      return undefined;
    }

    const eventSource = notificationService.subscribe({
      eventName: 'Notification',
      onOpen: async () => {
        notificationStore.setSubscribed(true);
        try {
          const count = await notificationService.getNotificationCount();
          notificationStore.setUnreadCount(count?.count ?? 0);
          if (openRef.current) {
            const list = await notificationService.getNotificationList(pageRef.current);
            setNotifications(list ?? []);
            setTotalCount(count?.count ?? 0);
          }
        } catch {
          if (openRef.current) setHasError(true);
        }
      },
      onMessage: (event) => {
        const notification = parseNotificationEvent(event);

        if (!notification || !notification.notificationId || seenRef.current.has(notification.notificationId)) {
          return;
        }
        seenRef.current.add(notification.notificationId);
        if (seenRef.current.size > 200) {
          seenRef.current.delete(seenRef.current.values().next().value);
        }

        const toastId = notification.notificationId;
        notificationStore.receive(notification);
        setToasts((currentToasts) => [...currentToasts, { id: toastId, notification }]);
        window.setTimeout(() => {
          setToasts((currentToasts) => currentToasts.filter((toast) => toast.id !== toastId));
        }, TOAST_VISIBLE_MS);
      },
      onError: () => {
        notificationStore.setSubscribed(false);
      },
    });

    return () => {
      eventSource.close();
      seenRef.current.clear();
      notificationStore.setSubscribed(false);
    };
  }, [isAuthenticated]);

  useEffect(() => {
    function handleDocumentClick(event) {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }

    document.addEventListener('click', handleDocumentClick);

    return () => {
      document.removeEventListener('click', handleDocumentClick);
    };
  }, []);

  const loadNotifications = async (page = currentPage) => {
    if (!isAuthenticated) {
      return;
    }

    setIsLoading(true);

    try {
      const [notificationList, countResponse] = await Promise.all([
        notificationService.getNotificationList(page),
        notificationService.getNotificationCount(),
      ]);

      setNotifications(notificationList ?? []);
      setTotalCount(countResponse?.count ?? 0);
      setCurrentPage(page);
      pageRef.current = page;
      setHasError(false);
    } catch {
      setNotifications([]);
      setHasError(true);
    } finally {
      setIsLoading(false);
    }
  };

  const toggleDropdown = async () => {
    const nextOpen = !isOpen;
    setIsOpen(nextOpen);
    openRef.current = nextOpen;

    if (nextOpen) {
      await loadNotifications(0);
    }
  };

  const moveToNotification = async (notification) => {
    if (notification.notificationId) {
      await notificationService.setAsRead(notification.notificationId);
    }

    setNotifications((currentNotifications) =>
      currentNotifications.map((item) =>
        item.notificationId === notification.notificationId ? { ...item, isRead: true, read: true } : item,
      ),
    );
    setIsOpen(false);
    navigate(buildRoute.boardDetail(notification.contentId));
  };

  const handleAllRead = async () => {
    await notificationService.setAllAsRead();
    setNotifications((currentNotifications) =>
      currentNotifications.map((notification) => ({ ...notification, isRead: true, read: true })),
    );
    notificationStore.setUnreadCount(0);
  };

  const movePage = async (page) => {
    if (page < 0 || page > endPage || page === currentPage) {
      return;
    }

    await loadNotifications(page);
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="notification-center" ref={rootRef}>
      <button aria-label={t('notifications.trigger')} className="notification-trigger" onClick={toggleDropdown} type="button">
        <svg aria-hidden="true" className="notification-icon" fill="none" viewBox="0 0 24 24">
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 8-3 9h18c0-1-3-2-3-9ZM10 21h4" stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.7" />
        </svg>
        {unreadCount > 0 ? <span className="notification-badge">{unreadCount}</span> : null}
      </button>

      {isOpen ? (
        <div className="notification-dropdown">
          {isLoading ? (
            <div className="notification-list-skeleton" role="status">
              <span className="visually-hidden">{t('notifications.loading')}</span>
              {[0, 1, 2].map((index) => <span aria-hidden="true" className="skeleton-block" key={index} />)}
            </div>
          ) : null}
          {!isLoading && hasError ? <p className="notification-message error">{t('notifications.error')}</p> : null}
          {!isLoading && !hasError && notifications.length === 0 ? (
            <p className="notification-message">{t('notifications.empty')}</p>
          ) : null}
          {(!isLoading ? notifications : []).map((notification) => {
            const isRead = notification.isRead ?? notification.read;

            return (
              <button
                className={isRead ? 'notification-item read' : 'notification-item unread'}
                key={notification.notificationId}
                onClick={() => moveToNotification(notification)}
                type="button"
              >
                {getNotificationMessage(notification, t)}
              </button>
            );
          })}
          <div className="notification-dropdown-footer">
            <button className="notification-all-read" onClick={handleAllRead} type="button">
              {t('notifications.allRead')}
            </button>
            <div className="notification-pages">
              <button
                aria-label={t('common:actions.previousPage')}
                disabled={currentPage <= 0}
                onClick={() => movePage(currentPage - 1)}
                type="button"
              >
                &lt;
              </button>
              <button
                aria-label={t('common:actions.nextPage')}
                disabled={currentPage >= endPage}
                onClick={() => movePage(currentPage + 1)}
                type="button"
              >
                &gt;
              </button>
            </div>
          </div>
        </div>
      ) : null}

      <div className="notification-toast-area" aria-live="polite">
        {toasts.map(({ id, notification }) => (
          <button
            className="notification-toast"
            key={id}
            onClick={() => moveToNotification(notification)}
            type="button"
          >
            <span>{getNotificationMessage(notification, t)}</span>
            <span
              className="notification-toast-close"
              aria-label={t('notifications.close')}
              onClick={(event) => {
                event.stopPropagation();
                setToasts((currentToasts) => currentToasts.filter((toast) => toast.id !== id));
              }}
            >
              x
            </span>
          </button>
        ))}
      </div>
    </div>
  );
}

import { useEffect, useMemo, useRef, useState } from 'react';
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

function getNotificationMessage(notification) {
  const publisherName = getPublisherName(notification);

  if (notification?.type === 'COMMENT') {
    return `${publisherName} 님이 내 글에 댓글을 달았습니다.`;
  }

  if (notification?.type === 'LIKE') {
    return `${publisherName} 님이 내 글에 좋아요를 눌렀습니다.`;
  }

  return '새 알림이 도착했습니다.';
}

function parseNotificationEvent(event) {
  try {
    return JSON.parse(event.data);
  } catch {
    return null;
  }
}

export default function NotificationCenter() {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const unreadCount = useNotificationStore((state) => state.unreadCount);
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [toasts, setToasts] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const rootRef = useRef(null);

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
      return undefined;
    }

    const eventSource = notificationService.subscribe({
      eventName: 'Notification',
      onMessage: (event) => {
        const notification = parseNotificationEvent(event);

        if (!notification) {
          return;
        }

        const toastId = `${Date.now()}-${notification.contentId ?? 'notification'}`;
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

    notificationStore.setSubscribed(true);

    return () => {
      eventSource.close();
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
      setErrorMessage('');
    } catch {
      setNotifications([]);
      setErrorMessage('알림을 불러오지 못했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const toggleDropdown = async () => {
    const nextOpen = !isOpen;
    setIsOpen(nextOpen);

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
      <button className="notification-trigger" onClick={toggleDropdown} type="button">
        알림
        {unreadCount > 0 ? <span className="notification-badge">{unreadCount}</span> : null}
      </button>

      {isOpen ? (
        <div className="notification-dropdown">
          {isLoading ? <p className="notification-message">알림을 불러오는 중입니다.</p> : null}
          {errorMessage ? <p className="notification-message error">{errorMessage}</p> : null}
          {!isLoading && !errorMessage && notifications.length === 0 ? (
            <p className="notification-message">알림 내역이 없습니다.</p>
          ) : null}
          {notifications.map((notification) => {
            const isRead = notification.isRead ?? notification.read;

            return (
              <button
                className={isRead ? 'notification-item read' : 'notification-item unread'}
                key={notification.notificationId}
                onClick={() => moveToNotification(notification)}
                type="button"
              >
                {getNotificationMessage(notification)}
              </button>
            );
          })}
          <div className="notification-dropdown-footer">
            <button className="notification-all-read" onClick={handleAllRead} type="button">
              모두 읽음
            </button>
            <div className="notification-pages">
              <button disabled={currentPage <= 0} onClick={() => movePage(currentPage - 1)} type="button">
                &lt;
              </button>
              <button disabled={currentPage >= endPage} onClick={() => movePage(currentPage + 1)} type="button">
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
            <span>{getNotificationMessage(notification)}</span>
            <span
              className="notification-toast-close"
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

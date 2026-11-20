import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import i18n from '../../i18n/index.js';
import { authStore } from '../../store/authStore.js';
import { notificationStore } from '../../store/notificationStore.js';
import { notificationService } from '../../services/notificationService.js';
import NotificationCenter from './NotificationCenter.jsx';

let handlers;

beforeEach(async () => {
  await i18n.changeLanguage('en');
  authStore.setAccessToken('test-token');
  notificationStore.setUnreadCount(0);
  vi.spyOn(notificationService, 'subscribe').mockImplementation((callbacks) => {
    handlers = callbacks;
    return { close: vi.fn() };
  });
  vi.spyOn(notificationService, 'getNotificationCount').mockResolvedValue({ count: 1 });
  vi.spyOn(notificationService, 'getNotificationList').mockResolvedValue([]);
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  authStore.clear();
});

it('재연결에서 건수와 열린 목록을 다시 조회하고 중복 알림을 표시하지 않는다', async () => {
  render(<MemoryRouter><NotificationCenter /></MemoryRouter>);
  await act(async () => { await handlers.onOpen(); });
  expect(notificationService.getNotificationCount).toHaveBeenCalledTimes(1);

  fireEvent.click(screen.getByRole('button', { name: /Notifications/ }));
  await waitFor(() => expect(notificationService.getNotificationList).toHaveBeenCalledTimes(1));
  await act(async () => { await handlers.onOpen(); });
  expect(notificationService.getNotificationCount).toHaveBeenCalledTimes(3);
  expect(notificationService.getNotificationList).toHaveBeenCalledTimes(2);

  const event = { data: JSON.stringify({ notificationId: 9, contentId: 'board-1', pubNickname: 'Mina', type: 'LIKE', isRead: false }) };
  act(() => { handlers.onMessage(event); handlers.onMessage(event); });
  expect(screen.getAllByText('Mina liked your tag note.')).toHaveLength(1);
});

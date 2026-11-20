import { afterEach, describe, expect, it, vi } from 'vitest';
import { notificationService } from './notificationService.js';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('알림 SSE', () => {
  it('최초 연결과 재연결에서 open 콜백을 호출한다', () => {
    const listeners = new Map();
    const source = {
      addEventListener: vi.fn((name, listener) => listeners.set(name, listener)),
      close: vi.fn(),
    };
    const EventSource = vi.fn(function EventSourceMock() { return source; });
    vi.stubGlobal('EventSource', EventSource);
    const onOpen = vi.fn();

    const connection = notificationService.subscribe({ eventName: 'Notification', onOpen });
    listeners.get('open')();
    listeners.get('open')();

    expect(EventSource).toHaveBeenCalledOnce();
    expect(listeners.has('Notification')).toBe(false);
    expect(onOpen).toHaveBeenCalledTimes(2);
    expect(connection).toBe(source);
  });
});

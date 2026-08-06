import { createStore } from './createStore.js';

const store = createStore({
  unreadCount: 0,
  latestNotification: null,
  isSubscribed: false,
});

export const notificationStore = {
  ...store,

  setUnreadCount(unreadCount) {
    store.setState({ unreadCount });
  },

  receive(notification) {
    store.setState((state) => ({
      latestNotification: notification,
      unreadCount: state.unreadCount + 1,
    }));
  },

  setSubscribed(isSubscribed) {
    store.setState({ isSubscribed });
  },
};

export function useNotificationStore(selector) {
  return notificationStore.useStore(selector);
}

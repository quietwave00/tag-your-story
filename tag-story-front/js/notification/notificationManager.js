import { setAsRead } from './notificationApi.js';

/**
 * 실시간 알림 메시지를 보여준다.
 * 
 * @param beforeNotification: 알림 내용(문자열)
 */
let notificationCount = 0;

const renderNotification = (beforeNotification) => {
  const notification = JSON.parse(beforeNotification);

  /* 알림 div */
  const notificationDiv = document.createElement("div");
  notificationDiv.className = "notification-div";
  notificationDiv.id = `notification-${notificationCount++}`;

  notificationDiv.textContent = getNotificationMessage(notification);
  notificationDiv.style.top = `${10 + notificationCount * 80}px`;

  notificationDiv.addEventListener('click', () => {
    setAsRead(notification.notificationId);
    window.location.href = `${client_host}/board.html?boardId=${notification.contentId}`;
  });

  /* 닫기 버튼 */
  const closeButton = document.createElement("span");
  closeButton.textContent = "×";
  closeButton.className = "notification-close-button";

  closeButton.addEventListener("click", () => {
      document.body.removeChild(notificationDiv);
      notificationCount--;
  });

  notificationDiv.appendChild(closeButton);
  document.body.appendChild(notificationDiv);

  setOpacity(notificationDiv);
}

/**
 * 알림 메시지 내용을 가공하여 돌려준다.
 * 
 * @param notification: 알림 json 
 * @returns 가공된 알림 메시지
 */
const getNotificationMessage = (notification) => {
  let message;
  
  const type = notification.type;
  if(type === "COMMENT") {
    message = `${notification.publisher.nickname} 님이 내 글에 댓글을 달았습니다.`;
  }

  if(type === "LIKE") {
    message = `${notification.publisher.nickname} 님이 내 글에 좋아요를 눌렀습니다.`;
  }

  return message;
}

/**
 * 알림 div에 fade-out 효과를 적용한다.
 * 
 * @param notificationDiv: 알림 div 
 */
const setOpacity = (notificationDiv) => {
  const fadeOutInterval = 30;
  const fadeOutDuration = 5000;
  const steps = fadeOutDuration / fadeOutInterval;

  let currentStep = 0;
  const opacityStep = 1 / steps;

  const fadeOutTimer = setInterval(() => {
    currentStep++;
    notificationDiv.style.opacity = 1 - currentStep * opacityStep;

    if (currentStep >= steps) {
      clearInterval(fadeOutTimer);

      if (document.body.contains(notificationDiv)) {
        document.body.removeChild(notificationDiv);
        notificationCount--;
      }
    }
  }, fadeOutInterval);
}

/**
 * Export EventSource 
 */
const authorizationToken = localStorage.getItem("Authorization");
export const eventSource = authorizationToken
  ? new EventSource(`${server_host}/api/notification/subscription?AccessToken=${authorizationToken}`)
  : null;

export { renderNotification };
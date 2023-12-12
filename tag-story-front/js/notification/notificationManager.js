/**
 * 알림을 그려준다.
 * 
 * @param notification: 알림 내용 
 */
const renderNotification = (notification) => {
  const notificationDiv = document.createElement("div");

  notificationDiv.textContent = notification;

  notificationDiv.style.position = "fixed";
  notificationDiv.style.top = "10px";
  notificationDiv.style.right = "10px";
  notificationDiv.style.backgroundColor = "lightblue";
  notificationDiv.style.padding = "10px";
  notificationDiv.style.border = "1px solid #ccc";

  document.body.appendChild(notificationDiv);

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
      document.body.removeChild(notificationDiv);
    }
  }, fadeOutInterval);
}

const authorizationToken = localStorage.getItem("Authorization");

export const eventSource = authorizationToken
  ? new EventSource(`${server_host}/api/notification/subscription?AccessToken=${authorizationToken}`)
  : null;

export { renderNotification };
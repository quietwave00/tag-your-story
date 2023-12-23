import NotificationApi from './notificationApi.js';
import { setAsRead } from './notificationApi.js';

const notificationIcon = document.getElementById('notification-icon');
let currentPage = 0;
let pageSize = 5;
let endPage;
/**
 * 알림 아이콘 클릭 이벤트
 */
notificationIcon.addEventListener('click', () => {
    NotificationApi.getNotificationCount().then((response) => {
        const perPage = response.count / pageSize;
        (perPage < 1) ? endPage = 1 : endPage = Math.ceil(perPage);
        endPage--;
    });
    renderNotificationList();
});

/**
 * 알림 리스트를 보여준다.
 */
const renderNotificationList = () => {
    /* 알림 drop-down */
    if(!document.getElementById('notification-dropdown')) {
        const notificationDropdown = document.createElement("div");
        notificationDropdown.id = "notification-dropdown";
        notificationIcon.parentNode.appendChild(notificationDropdown);
    }

    /* 데이터 */
    const dropDown = document.getElementById('notification-dropdown');

    NotificationApi.getNotificationList(currentPage).then((notificationList) => {
        dropDown.innerHTML = "";
        
        if(notificationList.length == 0) {
            dropDown.innerHTML = "알림 내역이 없습니다.";
        }

        notificationList.forEach(notification => {
            const notificationLink = `${client_host}/board.html?boardId=${notification.contentId}`;

            const notificationElement = document.createElement('p');
            notificationElement.className = notification.read ? 'readed' : 'un-read';
            notificationElement.id = `notification-${notification.notificationId}`;
            notificationElement.innerHTML = getNotificationMessage(notification);

            notificationElement.addEventListener('click', () => {
                setAsRead(notification.notificationId);
                window.location.href = notificationLink;
            });

            dropDown.appendChild(notificationElement);
        });

        /* 페이징 */
        const notificationPrev = document.createElement('span');
        notificationPrev.id = 'notification-prev';
        notificationPrev.textContent = '<';
        dropDown.appendChild(notificationPrev);

        notificationPrev.addEventListener('click', () => {
            if(currentPage > 0) {
                currentPage--;
                renderNotificationList();
            }
        });

        const notificationNext = document.createElement('span');
        notificationNext.id = 'notification-next';
        notificationNext.textContent = '>';
        dropDown.appendChild(notificationNext);

        notificationNext.addEventListener('click', () => {
            if (currentPage < endPage) {
                currentPage++;
                renderNotificationList();
            }
        });
    });

    manageDropdown();
}

/**
 * 알림 영역 드롭다운 이벤트
 */
const manageDropdown = () => {
    const notificationArea = document.getElementById("notification-area");
    const dropdown = document.getElementById("notification-dropdown");

    notificationArea.addEventListener('click', () => {
        if (dropdown.style.display === "none" || dropdown.style.display === "") {
            dropdown.style.display = "block";
        } else {
            dropdown.style.display = "none";
        }
    });

    dropdown.addEventListener('click', (event) => {
        event.stopPropagation();
    });

    document.addEventListener('click', (event) => {
        if (!notificationArea.contains(event.target)) {
            dropdown.style.display = "none";
        }
    });
}

/**
 * 알림 메시지 내용을 가공하여 돌려준다.
 * 
 * @param notification: 알림 내용
 * @returns 가공된 알림 메시지
 */
const getNotificationMessage = (notification) => {
    let message;

    const type = notification.type;
    if(type === "COMMENT") {
        message = `${notification.pubNickname} 님이 내 글에 댓글을 달았습니다.`;
    }

    if(type === "LIKE") {
        message = `${notification.pubNickname} 님이 내 글에 좋아요를 눌렀습니다.`;
    }
    return message;
}

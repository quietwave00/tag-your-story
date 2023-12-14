/**
 * 알림 읽음 처리를 요청한다.
 * 
 * @param notificationId: 알림 아이디
 */
const setAsRead = (notificationId) => {
    fetch(`${server_host}/api/notification`,{
        method:"PATCH",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        },
        body: JSON.stringify({
            "notificationId": notificationId
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success == false) {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    setAsRead(notificationId);
                });
        }
    });
}

/**
 * 알림 리스트 조회를 요청한다.
 * 
 * @param page: 알림 페이지 
 * @returns 
 */
const getNotificationList = (page) => {
    return fetch(`${server_host}/api/notification?page=${page}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        }
    })
    .then((res) => res.json())
    .then(res => {
        if (res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    getNotificationList(page);
                });
        }
    });
}

/**
 * 로그인 유저에 해당하는 전체 알림 개수를 요청한다.
 * 
 * @returns Subscriber에 대한 전체 알림 개수
 */
const getNotificationCount = () => {
    return fetch(`${server_host}/api/notification/count`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        }
    })
    .then((res) => res.json())
    .then(res => {
        if (res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    getNotificationCount();
                });
        }
    });
}

export { setAsRead };
export default {
    getNotificationList,
    getNotificationCount
}
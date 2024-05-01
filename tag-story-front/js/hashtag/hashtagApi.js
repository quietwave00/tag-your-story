/**
 * 최근 작성된 해시태그 리스트를 요청한다.
 */
const getRecentHashtagList = () => {
    return fetch(`${server_host}/api/boardhashtags/recent`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json"
        }
    })
    .then((res) => res.json())
    .then(res => {
        if (res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode);
        }
    });
}

export default {
    getRecentHashtagList
}

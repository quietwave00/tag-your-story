const setAsRead = (notificationId) => {
    return fetch(`${server_host}/api/notification`,{
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

export { setAsRead };
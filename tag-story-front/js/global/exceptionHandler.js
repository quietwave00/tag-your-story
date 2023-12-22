/**
 * 서버로부터 반환받은 ExceptionCode에 따라 예외를 처리해준다.
 * 
 * @param exceptionCode 
 */
const handleException = (exceptionCode) => {
    switch (exceptionCode) {
        case 'TOKEN_HAS_EXPIRED':
            return handleExpiredAccessToken();
        default:
            window.location.href = `${client_host}/exception.html`;
    }
}

/**
 * 만료된 AccessToken으로 요청 시 실행된다.
 * 새로운 AccessToken을 설정한다.
 * @param _refreshToken : 리프레쉬 토큰
 */
const handleExpiredAccessToken = () => {
    return fetch(`${server_host}/api/user/reissue/accessToken`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('RefreshToken')
        },
        body: JSON.stringify({
            "refreshToken": localStorage.getItem('RefreshToken')
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            console.log("handleExpiredAccessToken() Success");
            setAccessToken(res.response.newJwt);
        } else {
            if(res.exceptionCode === "TOKEN_HAS_EXPIRED") {
                handleExpiredRefreshToken();
            }
        }
    });
}

/**
 * 만료된 RefreshToken으로 요청 시 실행된다.
 * 새로운 RefreshToken을 설정한다.
 */
const handleExpiredRefreshToken = () => {
    fetch(`${server_host}/api/user/reissue/refreshToken`, {
        method: "POST",
        headers: {
            "Authorization": localStorage.getItem('Authorization')
        },
        body: JSON.stringify({
            "refreshToken": localStorage.getItem('RefreshToken')
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.result === true) {
            console.log("handleExpiredRefreshToken() Success");
            setRefreshToken(res.response.newJwt);
        } else {
            handleException(res.exceptionCode);
        }
    });
}

/**
 *  발급받은 AccessToken을 설정한다.
 * 
 * @param accessToken: 새로운 AccessToken
 */
const setAccessToken = (accessToken) => {
    localStorage.removeItem('Authorization');
    localStorage.setItem('Authorization', accessToken);
}

/**
 *  발급받은 RefreshToken을 설정한다.
 * 
 * @param refreshToken: 새로운 RefreshToken
 */
const setRefreshToken = (refreshToken) => {
    localStorage.removeItem('RefreshToken');
    localStorage.setItem('RefreshToken', refreshToken);
}

export default {
    handleException
};
/**
 * 서버로부터 반환받은 ExceptionCode에 따라 예외를 처리해준다.
 * 
 * @param exceptionCode 
 */
const handleException = (exceptionCode) => {
    console.log("handleException 들아옴");
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
            setAccessToken(res.response.token);
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
    console.log("RefreshToken executed");
    alert("로그인 인증 정보가 만료되었습니다. 다시 로그인 해주세요.");
    window.location.href = `${client_host}/login.html`;
}

/**
 *  발급받은 AccessToken을 설정한다.
 * 
 * @param accessToken: 새로운 AccessToken
 */
const setAccessToken = (accessToken) => {
    console.log("setAccessToken Executed");
    localStorage.removeItem('Authorization');
    localStorage.setItem('Authorization', accessToken);
}

/**
 *  발급받은 RefreshToken을 설정한다.
 * 
 * @param refreshToken: 새로운 RefreshToken
 */
const setRefreshToken = (refreshToken) => {
    console.log("setRefreshToken Executed");
    localStorage.removeItem('RefreshToken');
    localStorage.setItem('RefreshToken', refreshToken);
}

export default {
    handleException
};
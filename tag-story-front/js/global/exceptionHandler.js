/**
 * ExceptionCode에 따른 함수 정의 
 */
const handleEvents =  {
    TOKEN_HAS_EXPIRED: handleExpiredAccessToken(),
    NO_PERMISSION: handleNoPermission(),
    LOCKED_RESOURCE: handleLockedResource(),
}

/**
 * 서버로부터 반환받은 ExceptionCode에 따라 예외를 처리해준다.
 * 
 * @param exceptionCode 
 */
const handleException = async (exceptionCode) => {
    if(handleEvents[exceptionCode]) {
        handleEvents[exceptionCode]();
        return;
    }

    window.location.href = `${client_host}/exception.html`;
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
            setAccessToken(res.response.newAccessToken);
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
    alert("로그인 인증 정보가 만료되었습니다. 다시 로그인 해주세요.");
    window.location.href = `${client_host}/login.html`;
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
 * 인가 에러에 대한 처리를 수행한다.
 */
const handleNoPermission = () => {
    alert("로그인 후 이용해 주세요.");
}

/**
 * 락이 걸려 있는 자원 수정에 대한 요청을 처리한다.
 */
const handleLockedResource = () => {
    return false;
}

export default {
    handleException
};
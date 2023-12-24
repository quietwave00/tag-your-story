window.onload = () => {
    /**
     * 쿠키에 저장된 token값을 localStorage에 저장한다.
     */
    const authorizationCookieValue = getCookieValue('Authorization');
    const refreshTokenCookieValue = getCookieValue('RefreshToken');
    const pendingCookieValue = getCookieValue('Pending');
    
    if (authorizationCookieValue) {
        const extractedAuthorizationToken = extractBearerToken(authorizationCookieValue);
        localStorage.setItem('Authorization', extractedAuthorizationToken);
    }
    
    if (refreshTokenCookieValue) {
        const extractedRefreshToken = extractBearerToken(refreshTokenCookieValue);
        localStorage.setItem('RefreshToken', extractedRefreshToken);
    }
    
    if (pendingCookieValue && !authorizationCookieValue) {
        localStorage.setItem('Pending', pendingCookieValue);
    }
    window.location.href = `${client_host}/index.html`;
}

/**
 * 쿠키의 키 값에 따른 value를 반환해준다.
 * 
 * @param cookieName : 쿠키의 키 값
 * @returns 키 값에 따른 쿠키의 value
 */
const getCookieValue = (cookieName) => {
    const cookieString = document.cookie;
    const cookies = cookieString.split('; ');

    for (const cookie of cookies) {
        const [name, value] = cookie.split('=');
        if (name === cookieName) {
        return value;
        }
    }
    return null;
}

/**
 * token 값을 파싱해준다.
 * 
 * @param token : 쿠키의 value
 * @returns 파싱된 token
 */
const extractBearerToken = (token) => {
    const regex = /^Bearer\+/;
    return token.replace(regex, '');
}
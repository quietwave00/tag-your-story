window.onload = () => {
    /**
     * 쿠키에 저장된 token값을 localStorage에 저장한다.
     */
    const authorizationCookieValue = extractBearerToken(getCookieValue('Authorization'));
    const refreshTokenCookieValue = extractBearerToken(getCookieValue('RefreshToken'));
    localStorage.setItem('Authorization', authorizationCookieValue);
    localStorage.setItem('RefreshToken', refreshTokenCookieValue);
    console.log("rtk: " + refreshTokenCookieValue);
    console.log("atk: " + authorizationCookieValue);

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
 * @param authorizationValue : 쿠키의 value
 * @returns 파싱된 token
 */
const extractBearerToken = (authorizationValue) => {
    const regex = /^Bearer\+/;
    return authorizationValue.replace(regex, '');
}
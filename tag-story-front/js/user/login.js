/**
 * 로그인 요청을 한다.
 */
document.getElementById('login-button').addEventListener('click', () => {
    location.href = `${server_host}/oauth2/authorization/google`;
});
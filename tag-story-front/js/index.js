import UserArea from './user/userArea.js';

window.onload = () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();

    /**
     *  회원의 회원가입, 로그인 상태를 체크한다.
     */
    if(localStorage.getItem('Temp') != null) {
        window.location.href = `${client_host}/nickname.html`;
    }
}

/**
 * 검색 리스트로 이동
 */
document.getElementById('search-button').addEventListener('click', () => {
    let keyword = document.getElementById('search-input').value;
    const defaultPage = 1;
    window.location.href = `${client_host}/tracks.html?keyword=${keyword}&page=${defaultPage}`;
});
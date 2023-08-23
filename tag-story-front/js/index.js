import UserArea from './user/userArea.js';
import UserApi from './user/userApi.js';

window.onload = () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();

    /**
     *  회원의 회원가입, 로그인 상태를 체크한다.
     */
    if (localStorage.getItem('Authorization') != null) {
        UserApi.checkRegisterUser();
    }
}

/**
 * 검색 리스트로 이동
 */
document.getElementById('search-button').addEventListener('click', () => {
    let keyword = document.getElementById('search-input').value;
    let defaultPage = 0;
    window.location.href = `/html/track/tracks.html?keyword=${keyword}&page=${defaultPage}`;
});
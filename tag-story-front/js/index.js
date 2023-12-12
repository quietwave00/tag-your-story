import UserArea from './user/userArea.js';
import { trackManager } from './track/trackManager.js';
import { eventSource } from './notification/notificationManager.js'
import { renderNotification } from './notification/notificationManager.js';


window.onload = () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();

    /**
     *  회원의 회원가입, 로그인 상태를 체크한다.
     */
    if(localStorage.getItem('Pending') != null) {
        window.location.href = `${client_host}/nickname.html`;
    }

    if(eventSource) {
        eventSource.addEventListener('Notification', (e) => {
            renderNotification(e.data);
        });
    }
}
const searchInput = document.getElementById("search-input");
const searchButton = document.getElementById("search-button");

/**
 * 검색창에서 Enter를 누를 시 이벤트 리스너
 */
searchInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
        searchButton.click();
    }
});

/**
 * 검색 리스트로 이동
 */
searchButton.addEventListener('click', () => {
    const keyword = searchInput.value;
    const defaultPage = 1;
    trackManager.setKeyword(keyword);
    
    window.location.href = `${client_host}/tracks.html?keyword=${keyword}&page=${defaultPage}`;
});


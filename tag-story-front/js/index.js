import UserArea from './user/userArea.js';
import { trackManager } from './track/trackManager.js';
import { eventSource } from './notification/notificationManager.js'
import { renderNotification } from './notification/notificationManager.js';
import TrackApi from './track/trackApi.js';


window.onload = () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();

    /**
     * 실시간 알림을 수행한다.
     */
    if(eventSource) {
        eventSource.addEventListener('Notification', (e) => {
            renderNotification(e.data);
        });
    }

    /**
     *  회원의 회원가입, 로그인 상태를 체크한다.
     */
    if(localStorage.getItem('Pending') != null) {
        window.location.href = `${client_host}/nickname.html`;
    }

    /**
     * 검색어 랭킹을 보여준다.
     */
    TrackApi.getKeywordRanking().then((response) => {
        renderRankingArea(response);
    });
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
 * 랭킹 리스트 화면을 그려준다.
 */
const renderRankingArea = (response) => {
    const rankingArea = document.getElementById('keyword-ranking-area');
    const titleSpan = document.createElement('span');
    titleSpan.className = "ranking-area-title";
    titleSpan.textContent = "지금 많이 검색하는 노래";

    rankingArea.appendChild(titleSpan);
    const rankingList = response.rankingList.keywordList;
    rankingList.forEach((keyword, index) => {
        index++;
        

    });
}
/**
 * 검색 리스트로 이동
 */
searchButton.addEventListener('click', () => {
    const keyword = searchInput.value;
    const defaultPage = 1;
    trackManager.setKeyword(keyword);
    
    window.location.href = `${client_host}/tracks.html?keyword=${keyword}&page=${defaultPage}`;
});


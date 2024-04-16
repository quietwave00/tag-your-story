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
const defaultPage = 1;

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
    const rankingArea = document.getElementById('ranking-area');
    const titleSpan = document.createElement('span');
    titleSpan.className = "ranking-area-title";
    titleSpan.textContent = "지금 많이 검색하는 키워드";
    rankingArea.appendChild(titleSpan);

    const rankingList = response.keywordList;
    rankingList.forEach((keyword, index) => {
        index++;
        
        const keywordDiv = document.createElement('div');
        keywordDiv.className = "keyword-list";

        /* 순위 */
        const rankingSpan = document.createElement('span');
        rankingSpan.className = "ranking-index";
        rankingSpan.textContent = index;

        /* 키워드 */
        const keywordSpan = document.createElement('span');
        keywordSpan.className = "keyword";
        keywordSpan.textContent = keyword;

        /* 키워드 이벤트 리스너 */
        keywordSpan.addEventListener('click', () => {
            moveToSearchResult(keyword);
        })

        keywordDiv.appendChild(rankingSpan);
        keywordDiv.appendChild(keywordSpan);
        rankingArea.appendChild(keywordDiv);
    });
}

/**
 * 랭킹 키워드 클릭 시 검색 리스트로 이동한다.
 */
const moveToSearchResult = (keyword) => {
    trackManager.setKeyword(keyword);
    window.location.href = `${client_host}/tracks.html?keyword=${keyword}&page=${defaultPage}`;
}

/**
 * 검색 리스트로 이동
 */
searchButton.addEventListener('click', () => {
    const keyword = searchInput.value;
    trackManager.setKeyword(keyword);
    window.location.href = `${client_host}/tracks.html?keyword=${keyword}&page=${defaultPage}`;
});


import UserArea from "../user/userArea.js";
import TrackApi from "../track/trackApi.js";
import { trackManager } from "../track/trackManager.js";
import { eventSource } from '../notification/notificationManager.js';
import { renderNotification } from '../notification/notificationManager.js';

const pageSize = 10;
let currentPage = 1;
let endPage = 0;

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
     *  검색 키워드에 따른 목록을 보여준다.
     */
    const keyword = new URLSearchParams(window.location.search).get("keyword");
    const page = new URLSearchParams(window.location.search).get("page");
    TrackApi.searchTrack(keyword, page).then((response) => {
        renderTrackList(response.trackDataList);

        const perPage = response.totalCount / pageSize;
        (perPage < 1) ? endPage = 1 : endPage = Math.ceil(perPage);

        pagingTrackList();
    });
}

/**
 * 트랙 리스트를 렌더링한다.
 * @param trackList: 트랙 리스트, 전체 검색 결과 개수
 */
const renderTrackList = (trackList) => {
    document.getElementById('track-area').innerHTML = "";

    if(trackList.length == 0) {
        document.getElementById('track-area').innerHTML = 
            `
                <div style = "margin-left: 30%; margin-top: 10%;">검색 결과가 없습니다.</div>
            `;
    }

    for(let track of trackList) {
        let trackId = track.trackId;
        let title = track.title;
        let artist = track.artistName;
        let album = track.albumName;
        let imageUrl = track.imageUrl;

        document.getElementById('track-area').innerHTML += 
            `
                <div class = "row track">
                    <input class = "track-id" type = "hidden" value = ${trackId}>
                    <div class = "col-5">
                        <img src = "${imageUrl}" style = "width: 60%">
                    </div>
                    <div class = "col-7">
                        <div class = "row">
                            <div class = "col-7">
                                <span class = "title">${title}</span>
                            </div>
                            <div class = "col-7">
                                <span style = "color: gray; font-weight: bold;">Artist</span> <span class = "artist">${artist}</span>
                            </div>
                            <div class = "col-7">
                            <span style = "color: gray; font-weight: bold;">Album</span> <span class = "album">${album}</span>
                            </div>
                        </div>
                    </div>        
                </div> 
            `;
    }
    moveDetails();
}

/**
 *  페이징 관련 함수
 */
const pagingTrackList = () => {
    const prevButton = document.getElementById("prev-button");
    const nextButton = document.getElementById("next-button");
    const numberList = document.getElementById("number-list");

    if(prevButton && nextButton && numberList) {
        /**
         * 숫자 생성 및 페이지 업데이트
         */
        const updatePage = () => {
            numberList.innerHTML = "";

            const start = 1;
            for (let i = start; i <= endPage; i++) {
                const numberItem = document.createElement("span");
                numberItem.className = "page-number";
                numberItem.textContent = i;
                numberList.appendChild(numberItem);
                numberItem.addEventListener("click", () => onPageNumberClick(i));
            }
        }

        prevButton.addEventListener("click", () => {
            if (currentPage > 1) {
                currentPage--;
                onPageNumberClick(currentPage);
            }
        });

        nextButton.addEventListener("click", () => {
            if (currentPage < endPage) {
                currentPage++;
                onPageNumberClick(currentPage);
            }
        });
        updatePage();
    }
}

const onPageNumberClick = async (page) => {
    window.scrollTo({ top: 0, behavior: 'smooth' });

    const keyword = new URLSearchParams(window.location.search).get("keyword");
    await TrackApi.searchTrack(keyword, page).then((response) => {
        renderTrackList(response.trackDataList);
    });
}

/**
 *  트랙 영역 클릭 시 상세 페이지로 이동한다.
 */
const moveDetails = () => {
    const trackElements = document.getElementsByClassName('track');

    Array.from(trackElements).forEach(trackElement => {
        trackElement.addEventListener('click', (e) => {
            const trackId = e.currentTarget.querySelector('.track-id').value;
            const title = e.currentTarget.querySelector('.title').textContent;
            
            trackManager.setTrackId(trackId);
            trackManager.setTitle(title);
            
            window.location.href = `detail.html?trackId=${trackId}`;
        });
    });
}


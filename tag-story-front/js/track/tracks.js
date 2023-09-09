import UserArea from "../user/userArea.js";
import TrackApi from "../track/trackApi.js";

window.onload = () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();

    /**
     *  page-area에 대한 처리를 수행한다.
     */
    pagingTrackList();

    /**
     *  검색 키워드에 따른 목록을 보여준다.
     */
    const keyword = new URLSearchParams(window.location.search).get("keyword");
    const page = new URLSearchParams(window.location.search).get("page");
    TrackApi.searchTrack(keyword, page).then((response) => {renderTrackList(response)});
}

/**
 * 트랙 리스트를 렌더링한다.
 * @param trackList: 트랙 리스트
 */
const renderTrackList = (trackList) => {
    document.getElementById('track-area').innerHTML = "";
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

    const itemsPerPage = 10;
    let currentPage = 1;
    const totalItems = 100;

    /**
     * 숫자 생성 및 페이지 업데이트
     */
    function updatePage() {
        numberList.innerHTML = "";

        const start = (currentPage - 1) * itemsPerPage + 1;
        const end = Math.min(start + itemsPerPage, totalItems + 1);

        for (let i = start; i < end; i++) {
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
            updatePage();
        }
    });

    nextButton.addEventListener("click", () => {
        if (currentPage < Math.ceil(totalItems / itemsPerPage)) {
            currentPage++;
            updatePage();
        }
    });
    updatePage();
}

const onPageNumberClick = (page) => {
    const keyword = new URLSearchParams(window.location.search).get("keyword");
    TrackApi.searchTrack(keyword, page).then((response) => {
        renderTrackList(response)
    });
}

/**
 *  트랙 영역 클릭 시 상세 페이지로 이동한다.
 */
const moveDetails = () => {
    const trackElements = document.getElementsByClassName('track');

    for (let element of trackElements) {
        element.addEventListener('click', (e) => {
            const trackId = e.currentTarget.querySelector('.track-id').value;
            window.location.href = `detail.html?trackId=${trackId}`;
        });
    }
}


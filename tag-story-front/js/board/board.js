import BoardApi from "./boardApi.js"
import File from "./file.js"

/**
 * 해당 스크립트는 detail.html에서 detail.js와 함께 사용된다. 
 */
const trackId = new URLSearchParams(window.location.search).get('trackId');

/**
 *  게시물 리스트 렌더링 함수
 */
const renderBoardList = (boardList) => {
    console.log("게시글 그려짐");
    document.getElementById('board-element-area').innerHTML = "";
    if(boardList.length === 0) {
        document.getElementById('board-message-area').innerHTML += 
            `
            <span style="text-align: center; margin-bottom: 50px;">작성된 게시글이 없습니다.</span>
            `;
    }
    for(let board of boardList) {
        let boardId = board.boardId;
        let hashtagList = board.hashtagNameList.nameList;
        let content = board.content;
        let hashtagElements = "";
        for(let hashtag of hashtagList) {
            hashtagElements += 
                `
                <div class = "hashtag-element">#${hashtag}</div>
                `;
        }
        document.getElementById('board-element-area').innerHTML += 
            `
            <div class = "col-5 board-element" id = "board-${boardId}">
                <div class = "hashtag-area">${hashtagElements}</div>
                <div class = "content-area">${content}</div>
            </div>
            `;
    }
    moveDetails();
}

/**
 * 게시글 작성 버튼 클릭 시 이벤트 함수
 */
document.getElementById('write-button').addEventListener('click', async () => {
    const resultHashtagArray = hashtagArray.filter(value => value !== undefined);
    const writeBoardResponse = await BoardApi.writeBoard(resultHashtagArray, trackId);
    hashtagArray = [];
    renderBoard(writeBoardResponse);
    if(document.getElementsByClassName('img_div').length > 0) {
        File.upload(writeBoardResponse.boardId).then((uploadResponse) => {
            const mainFileObject = [{
                "filePath": uploadResponse[0].filePath,
                "boardId": writeBoardResponse.boardId
            }];
            
            File.renderMainFileList(mainFileObject);
        });
    }
    renderAlert();
});

/**
 * 게시글 작성 완료 알림을 보여준다.
 */
const renderAlert = () => {
    document.getElementById('alert-area').innerHTML =
            `
                <div class = "alert alert-dark hide" role = "alert">
                    Your board has been created successfully.
                </div>
            `;
            let alertDiv = document.querySelector('.alert');
            let opacity = 1;
            let timer = setInterval(function() {
                if (opacity > 0) {
                    opacity -= 0.005;
                    alertDiv.style.opacity = opacity;
                } else {
                    clearInterval(timer);
                    alertDiv.style.display = 'none';
                }
            }, 10);
}

/**
 * 게시글 작성 응답값을 토대로 렌더링해준다.
 */
const renderBoard = (board) => {
    document.getElementById('board-message-area').innerHTML = "";
    let boardId = board.boardId;
    let hashtagList = board.hashtagList.nameList;
    let content = board.content;
    let hashtagElements = "";
    for(let hashtag of hashtagList) {
        hashtagElements += 
                `
                <div class = "hashtag-element">#${hashtag}</div>
                `;
    }
    const boardElementArea = document.getElementById('board-element-area');
    boardElementArea.insertAdjacentHTML('afterbegin', `
        <div class = "col-5 board-element" id = "board-${boardId}">
            <div class = "hashtag-area">${hashtagElements}</div>
            <div class="content-area">${content}</div>
        </div>
        `);
        document.getElementById('hashtag-container').innerHTML = "";
        document.getElementById('board-input').value = "";

        moveDetails();
}

/**
 *  페이징 관련 함수
 */
const pagingBoardList = () => {
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
    BoardApi.getBoardListByTrackId(trackId, page).then((response) => {
        renderBoardList(response)
    });
}


/**
 *  page-area에 대한 처리를 수행한다.
 */
pagingBoardList();

/**
 * 게시글의 상세 페이지로 이동한다.
 */
const moveDetails = () => {
    const boardElements = document.getElementsByClassName('board-element');

    for(let board of boardElements) {
        board.addEventListener('click', (e) => {
            const boardId = e.currentTarget.querySelector('.board-id').value;
            window.location.href = `${client_host}/board.html?boardId=${boardId}`;
        });
    }
}

export default {
    renderBoardList
}
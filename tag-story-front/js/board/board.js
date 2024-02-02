import BoardApi from "./boardApi.js";
import File from "./file.js";
import FileApi from "./fileApi.js";
import { hashtagArray as hashtagArrayFromModule } from "./hashtag.js";
import Hashtag from "./hashtag.js";

/**
 * 해당 스크립트는 detail.html에서 detail.js와 함께 사용된다. 
 */

const trackId = new URLSearchParams(window.location.search).get('trackId');
const pageSize = 8;
const defaultPage = 1;
const defaultOrderType = "CREATED_AT";
let orderType = "CREATED_AT";
let currentPage = 1;
let endPage = 0;

const setUp = async () => {
    /**
     * 게시물 리스트를 가져온다.
     */
    await BoardApi.getBoardListByTrackId(trackId, defaultOrderType, defaultPage).then((response) => {
        const perPage = response.totalCount / pageSize;
        (perPage < 1) ? endPage = 1 : endPage = Math.ceil(perPage);
        
        /**
         *  page-area에 대한 처리를 수행한다.
         */
        pagingBoardList();
        renderBoardList(response.boardResponseList);
    });
    

    /**
     * 메인 이미지 파일을 가져온다.
     */
    await FileApi.getMainFileList(trackId, defaultPage).then((response) => {
        File.renderMainFileList(response);
    });
}

/**
 *  게시물 리스트 렌더링 함수
 */
const renderBoardList = (boardList) => {
    document.getElementById('board-element-area').innerHTML = "";
    const boardMessageArea = document.getElementById('board-message-area');
    if(boardMessageArea) {
        boardMessageArea.innerHTML = "";
    }
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
 * 선택한 정렬 방식에 따라 orderType을 설정한다.
 */
const orderButtons = document.getElementsByName('boardOrderType');
orderButtons.forEach((button) => {
    button.addEventListener('click', async (e) => {
        orderType = e.currentTarget.value;

        await BoardApi.getBoardListByTrackId(trackId, orderType, currentPage).then((response) => {
            renderBoardList(response.boardResponseList);
        });

        await FileApi.getMainFileList(trackId, currentPage).then((response) => {
            File.renderMainFileList(response)});
    });
});
    
    

/**
 * 게시글 작성 버튼 클릭 시 이벤트 함수
 */
const writeButton = document.getElementById('write-button');
if(writeButton) {
    writeButton.addEventListener('click', async () => {
        if(hashtagArrayFromModule.length == 0) {
            alert("해시태그를 하나 이상 작성해 주셔야 합니다.");
            return;
        }
        const writeBoardResponse = await BoardApi.writeBoard(hashtagArrayFromModule, trackId);
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
        Hashtag.clearHashtagArray();
    });
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
    await BoardApi.getBoardListByTrackId(trackId, orderType, page).then((response) => {
        renderBoardList(response.boardResponseList)
    });

    await FileApi.getMainFileList(trackId, page).then((response) => {
        if(response.length > 0) {
            File.renderMainFileList(response.boardList)        
        }
    });
}

/**
 * 게시글의 상세 페이지로 이동한다.
 */
const moveDetails = () => {
    const boardElements = document.getElementsByClassName('board-element');

    for(let board of boardElements) {
        board.addEventListener('click', (e) => {
            const prefix = "board-";
            const boardElement = e.target.closest('.board-element');
            if (boardElement) {
                const boardId = boardElement.getAttribute('id').replace(prefix, '');
                window.location.href = `${client_host}/board.html?boardId=${boardId}`;
            }
        });
    }
}

export { renderBoardList }
export default { setUp }

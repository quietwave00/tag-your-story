import UserArea from "../user/userArea.js";
import Hashtag from "./hashtag.js";
import BoardApi from "./boardApi.js";
import File from "./board_detail_file.js";
import { hashtagArray as hashtagArrayFromModule } from "./hashtag.js";
import { editFlag as hashtagEditFlag } from "./hashtag.js";
import { editFlag as fileEditFlag } from "./board_detail_file.js";


window.onload = () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();

    /**
     * 사용자가 작성했던 게시글의 정보를 가져온다.
     */
    getExistedData();

    /**
     * 기존 데이터를 보여준다.
     */
    renderExistedBoard();

    /* URL 초기화 */
    // window.history.pushState({}, '', `${client_host}/edit.html`);
};

let hashtagArray = [];
let content;
let boardId;
/**
 * 기존 데이터를 보여주는 함수
 */
const renderExistedBoard = () => {
    let hashtagElements = "";
    for(let hashtag of hashtagArray) {
        hashtagElements += 
            `
                <div class = "hashtag-element">#${hashtag}</div>
            `;
    }//here
    document.getElementById("hashtag-container").innerHTML = `${hashtagElements}`;
    document.getElementById("board-input").innerHTML = `${content}`;

    /* 해시태그 상태 관리 */
    Hashtag.addIdToTagElements();
    Hashtag.addTagToHashtagArray(hashtagArray);
}

/**
 * 수정 버튼 클릭 이벤트 함수
 */
document.getElementById("edit-button").addEventListener('click', () => {
    content = document.getElementById("board-input").value;
    const resultHashtagArray = hashtagEditFlag ? hashtagArrayFromModule : new Array();
    BoardApi.updateBoardAndHashtag(boardId, content, resultHashtagArray);

    if(fileEditFlag) {
        File.deleteAndUpdateFile();
    }
    
    location.href = `${client_host}/board.html?boardId=${boardId}`;
});

/**
 * 사용자가 작성했던 게시글의 정보를 가져오는 함수
 */
const getExistedData = () => {
    const queryString = window.location.search;
    const urlParams = new URLSearchParams(queryString);
    if(urlParams.get('hashtags')) {
        const hashtags = urlParams.get('hashtags');

        hashtagArray = hashtags.split(',').map(hashtag => hashtag.trim());
        content = urlParams.get('content').trim();
        boardId = urlParams.get('boardId');
    } else {
        window.history.back();
        window.history.back();
    }
}

export { boardId };
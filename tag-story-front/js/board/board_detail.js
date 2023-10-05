import UserArea from "../user/userArea.js";
import BoardApi from "../board/boardApi.js"
import Like from "../board/like.js";

window.onload = () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();

    const boardId = new URLSearchParams(window.location.search).get('boardId');
    /**
     * 상세 게시글 정보를 요청하고 렌더링한다.
     */
    BoardApi.getBoardByBoardId(boardId).then((response) => {
        renderBoard(response)
    });

    /**
     * 사용자의 게시글 좋아요 여부를 검사한다.
     */
    if(localStorage.getItem("Authorization") != null) {
        Like.checkLiked(boardId);
    }

    /**
     * 좋아요 개수를 요청한다.
     */
    Like.getLikeCount(boardId);
};

/**
 * 상세 게시글 정보를 보여준다. 
 */
const renderBoard = (board) => {
    let content = board.content;
    let nickname = board.nickname;
    let createdAt = board.createdAt;
    let hashtagList = board.hashtagNameList.nameList;

    let hashtagElements = "";
    for(let hashtag of hashtagList) {
        hashtagElements += 
            `
            <div class = "tag-elements" onclick="getBoardListByHashtag(\'${hashtag}\')" data-bs-toggle="modal" data-bs-target="#board-hashtag-modal">#${hashtag}</div>
            `;
    }

    document.getElementById('board-area').innerHTML =
        `
            <div id = "board-element">
                <div class = "row">
                    <div class = "col-9" id = "hashtag-area">${hashtagElements}</div>
                    <div class = "col-3" id = "created-date-area">${createdAt}</div>
                </div>
                
                <div id = "content-area">${content}</div>
                <div id = "user-area">${nickname}</div>
            </div>
        `;
}
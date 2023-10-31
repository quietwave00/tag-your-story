import UserArea from "../user/userArea.js";
import BoardApi from "../board/boardApi.js"
import Like from "../board/like.js";
import Comment from "../comment/comment.js"

window.onload = () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();

    const boardId = new URLSearchParams(window.location.search).get('boardId');
    /*
     * 상세 게시글 정보를 요청하고 렌더링한다.
     */
    BoardApi.getBoardByBoardId(boardId).then((response) => {
        renderBoard(response)
    });

    /*
     * 댓글 리스트를 보여준다.
     */
    Comment.getCommentList(boardId);

    /**
     * 게시글 작성자인지 확인한다.
     */
    if(localStorage.getItem('Authorization')) {
        const isWriter = BoardApi.isWriter(boardId);
        if(isWriter) {
            renderEditBoardArea();
        }
    }
    

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

let content;
let nickname;
let createdAt;
let hashtagList;
/**
 * 상세 게시글 정보를 보여준다. 
 */
const renderBoard = (board) => {
    content = board.content;
    nickname = board.nickname;
    createdAt = board.createdAt[0] + "." + board.createdAt[1] + "." + board.createdAt[2];
    hashtagList = board.hashtagNameList.nameList;

    let hashtagElements = "";
    for(let hashtag of hashtagList) {
        hashtagElements += 
            `
            <div class = "hashtag-elements" data-bs-toggle="modal" data-bs-target="#board-hashtag-modal">#${hashtag}</div>
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

/**
 * 게시물 수정, 삭제 버튼을 보여준다.
 */
const renderEditBoardArea = () => {
    const boardId = new URLSearchParams(window.location.search).get('boardId');
    document.getElementById("board-edit-area").innerHTML =
        `
            <span id="edit-board" data-bs-toggle="modal" data-bs-target="#edit-board-modal">수정</span>
            <span id="delete-board">삭제</span>
        `;
        const deleteBoardElement = document.getElementById('delete-board');
        deleteBoardElement.onclick = () => {
            deleteBoard(boardId);
        };
    document.getElementById("edit-board-modal").addEventListener("shown.bs.modal", renderExistedBoard);
};

/**
 * 수정 시 기존 데이터를 보여준다.
 */
const renderExistedBoard = () => {
    let hashtagElements = "";
    for(let hashtag of hashtagList) {
        hashtagElements += 
            `
                <div class = "editable-tag-elements">#${hashtag}</div>
            `;
    }
    document.getElementById("hashtag-container").innerHTML = `${hashtagElements}`;
    document.getElementById("board-input").innerHTML = `${content}`;
    // document.getElementById("uploaded_file_view").innerHTML = `${}`;

    addIdToTagElements();
    addTagToHashtagArray();
}

/**
 * 수정 버튼 클릭 이벤트 함수
 */
document.getElementById("edit-button").addEventListener('click', () => {
    const boardId = new URLSearchParams(window.location.search).get('boardId');
    const content = document.getElementById("board-input").value;
    const resultHashtagArray = hashtagArray.filter(value => value !== undefined);
    const response = BoardApi.updateBoardAndHashtag(boardId, content, resultHashtagArray);
    renderBoard(response);
});

/**
 * 게시글을 삭제한다.
 */
const deleteBoard = (boardId) => {
    BoardApi.deleteBoard(boardId);
}

/**
 * 해시태그 클릭 시 해당 태그가 포함된 게시글을 보여준다.
 */
const getBoardListByHashtagName = (hashtagName) => {
    BoardApi.getBoardListByHashtagName(hashtagName).then((response) => {
        console.log(response);
    });
}


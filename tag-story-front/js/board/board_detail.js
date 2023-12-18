import UserArea from "../user/userArea.js";
import BoardApi from "../board/boardApi.js"
import Like from "../board/like.js";
import Comment from "../comment/comment.js";
import Hashtag from "./hashtag.js";
import { trackManager } from "../track/trackManager.js"
import { eventSource } from '../notification/notificationManager.js'
import { renderNotification } from '../notification/notificationManager.js';

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

    const boardId = new URLSearchParams(window.location.search).get('boardId');
    /*
     * 상세 게시글 정보를 요청하고 렌더링한다.
     */
    BoardApi.getBoardByBoardId(boardId).then((response) => {
        renderBoard(response);
        Like.renderLikeCount(response.likeCount);
    });
    
    /**
     * 뒤로가기 버튼을 보여준다.
     */
    renderBackArea();

    /*
     * 댓글 리스트를 보여준다.
     */
    Comment.getCommentList(boardId)
        .then(() => {
            if (localStorage.getItem('Authorization')) {
                Comment.getUserCommentId();
            }
        });

    /**
     * 게시글 작성자인지 확인한다.
     */
    if(localStorage.getItem('Authorization')) {
        BoardApi.isWriter(boardId)
            .then((isWriter) => {
                if(isWriter) {
                    renderEditBoardArea();
                }
            });        
    }
    
    /**
     * 사용자의 게시글 좋아요 여부를 검사한다.
     */
    if(localStorage.getItem("Authorization") != null) {
        Like.checkLiked(boardId);
    }
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
    hashtagList.forEach(hashtag => { 
        hashtagElements +=
            `
            <div class="hashtag-element" data-bs-toggle="modal"data-bs-target="#board-hashtag-modal" data-hashtag="${hashtag}">#${hashtag}</div>
            `;
    });
    
    const modalElement = document.getElementById('board-hashtag-modal');
    modalElement.addEventListener('show.bs.modal', (e) => {
        const clickedHashtag = e.relatedTarget.getAttribute('data-hashtag');
        Hashtag.getBoardListByHashtag(clickedHashtag);
    });
    
    document.getElementById('hashtag-container').innerHTML = hashtagElements;

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
    const boardEditArea = document.getElementById("board-edit-area");
    if(boardEditArea) {
        boardEditArea.innerHTML =
            `
                <span id="edit-board">수정</span>
                <span id="delete-board">삭제</span>
            `;
    } 

    const editBoardElement = document.getElementById('edit-board');
    if(editBoardElement) {
        editBoardElement.onclick = () => {
            location.href = `${client_host}/edit.html?hashtags=${encodeURIComponent(hashtagList)}
                            &content=${encodeURIComponent(content)}
                            &boardId=${encodeURIComponent(boardId)}`;
        }
    }
    
    const deleteBoardElement = document.getElementById('delete-board');
    if(deleteBoardElement) {
        deleteBoardElement.onclick = () => {
            if(confirm("삭제하시겠습니까?")) {
                deleteBoard(boardId);
            }
        };
    }
};

/**
 * 게시글을 삭제한다.
 */
const deleteBoard = (boardId) => {
    BoardApi.deleteBoard(boardId);
}

/**
 * 돌아가기 버튼을 생성한다.
 */
const renderBackArea = () => {
    const title = trackManager.getTrackInfo().selectedTitle;
    const trackId = trackManager.getTrackInfo().selectedTrackId;
    document.getElementById('back-title').innerText = title;

    document.getElementById('back-area').addEventListener('click', () => {
        window.location.href = `${client_host}/detail.html?trackId=${trackId}`;
    });
}

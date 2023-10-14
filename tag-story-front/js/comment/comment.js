import CommentApi from './commentApi.js';

/* 해당 스크립트는 board.html에서 함께 사용된다. */

/**
 * 댓글 리스트를 요청하고 렌더링한다.
 */
const getCommentList = (boardId) => {
    CommentApi.getCommentList(boardId).then((response) => {
        if(response.length > 0) {
            renderCommentList(response);
        }
    });
}

/**
 * 답글 입력란을 보여준다.
 */
const renderReplyForm = (commentId) => {
    const parentElement = document.querySelector(`.comment-element input[value="${commentId}"]`).parentElement;
    const childDiv = document.createElement('div');
    childDiv.classList.add('child-div');
    childDiv.innerHTML = `
                                        <input type = "text" class = "reply-input" placeholder = "Write Reply">
                                        <button class = "btn btn-sm btn-dark reply-input-btn">write</button>
                                        `;
    parentElement.appendChild(childDiv);

    const replyButton = childDiv.querySelector('.reply-button');
    replyButton.addEventListener('click', (e) => {
        const parentNode = e.target.parentNode;
        const replyInput = parentNode.querySelector('input');
        // addReply(commentId, replyInput);
    });
}

/**
 * 댓글 입력 버튼 클릭 이벤트 함수
 */
document.getElementById("comment-write-button").addEventListener("click", () => {
    const content = document.getElementById('comment-input').value;
    const boardId = new URLSearchParams(window.location.search).get('boardId');
    CommentApi.writeComment(boardId, content).then((response) => {
        renderComment(response);
    })
});

/**
 * 댓글 리스트를 보여준다.
 */
const renderCommentList = (commentList) => {
    for(let comment of commentList) {
        document.getElementById('comment-elements').innerHTML +=
        `
        <div class = "row comment-element">
            <input type = "hidden" class = "comment-id" value = "${comment.commentId}">
            <div class = "col-2 comment-nickname">${comment.nickname}</div>
            <div class = "col-4 comment-content">${comment.content}
            <span class = "reply-button" onclick = "renderReplyForm(${comment.commentId})">↳</span>
            </div>
        </div>
        `;
    }
}

/**
 * 작성한 댓글을 보여준다.
 */
const renderComment = (comment) => {
    console.log("잉?");
    document.getElementById('comment-input').value = "";

    const existedCommentElements = document.getElementById('comment-elements');
    const newCommentElement = document.createElement('div');
    newCommentElement.className = 'row comment-element';

    newCommentElement.innerHTML = 
        `
        <input type = "hidden" class = "comment-id" value = "${comment.commentId}">
        <div class = "col-2 comment-nickname">${comment.nickname}</div>
        <div class = "col-4 comment-content">${comment.content}
        <span class = "reply-button" onclick = "renderReplyForm(${comment.commentId})">↳</span>
        </div>
        `;

        existedCommentElements.insertBefore(newCommentElement, existedCommentElements.firstChild);
}


export default {
    getCommentList
}
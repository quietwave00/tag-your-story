import CommentApi from './commentApi.js';

/* 해당 스크립트는 board.html에서 함께 사용된다. */


const boardId = new URLSearchParams(window.location.search).get('boardId');

/**
 * 댓글 리스트를 요청한다.
 */
const getCommentList = (boardId) => {
    return CommentApi.getCommentList(boardId)
        .then((response) => {
            if (response.length > 0) {
                renderCommentList(response);
            }
        });
}

/**
 * 답글 입력란을 보여준다.
 */
const renderReplyForm = (commentId) => {
    const parentElement = document.getElementById(`comment-${commentId}`);
    const childDiv = document.createElement('div');
    childDiv.classList.add('reply-area');
    childDiv.innerHTML = `
                        <input type = "text" class = "reply-input" placeholder = "Write Reply">
                        <button class = "btn btn-sm btn-dark reply-button">write</button>
                        `;
    parentElement.appendChild(childDiv);
    const replyButton = childDiv.querySelector('.reply-button');

    replyButton.addEventListener('click', (e) => {
        const parentNode = e.target.parentNode;
        const replyInput = parentNode.querySelector('input');

        createReply(commentId, replyInput.value);
    });
}

/**
 * 답글 작성을 요청한다.
 */
const createReply = (parentId, replyInput) => {
    CommentApi.createReply(boardId, parentId, replyInput).then((response) => {
        renderComment(response, false, true);
    });
    location.reload();
}

/**
 * 댓글 입력 버튼 클릭 이벤트 함수
 */
document.getElementById("comment-write-button").addEventListener("click", () => {
    const content = document.getElementById('comment-input').value;
    CommentApi.writeComment(boardId, content).then((response) => {
        renderComment(response, false, false);
    })
});

/**
 * 댓글 리스트를 보여준다.
 */
const renderCommentList = (commentWithReplyList) => {
    commentWithReplyList.forEach(commentWithReply => {
        renderComment(commentWithReply.comment, true, false);
        if(commentWithReply.children.length > 0) {
            commentWithReply.children.forEach(child => {
                renderComment(child, true, true);
            })
        }
    });
}


/**
 * 작성한 댓글을 보여준다.
 * @param comment: 댓글 정보 
 * @param isList: renderList()에서 호출되었는지 여부
 * @param isReply: 댓글의 성격이 답글인지 여부
 */
const renderComment = (comment, isList, isReply) => {
    const commentElements = document.getElementById('comment-elements');

    /* 댓글 엘리먼트 */
    const newCommentElement = document.createElement('div');
    newCommentElement.className = isReply ? 'row reply-element' : 'row comment-element';
    newCommentElement.id = `comment-${comment.commentId}`;

    /* 닉네임 */
    const commentNickname = document.createElement('div');
    commentNickname.className = 'col-2 comment-nickname';
    commentNickname.textContent = comment.user.nickname;

    /* 내용 */
    const commentContent = document.createElement('div');
    commentContent.className = 'col-4 comment-content';

    /* 답글 버튼 */
    const replyButton = document.createElement('span');
    replyButton.className = 'reply-button';
    replyButton.textContent = '↳';

    /* 답글 이벤트 */
    replyButton.addEventListener('click', () => {
        const parentId = isReply ? comment.parentId : comment.comentId;
        renderReplyForm(comment.commentId);
    });

    /* append */
    if (isReply) {
        const replySymbol = document.createTextNode('↳ ');
        commentContent.appendChild(replySymbol);
    }
    const textNode = document.createTextNode(comment.content);
    commentContent.appendChild(textNode);
    commentContent.appendChild(replyButton);

    /* 댓글 아이디 */
    const commentIdInput = document.createElement('input');
    commentIdInput.type = 'hidden';
    commentIdInput.className = 'comment-id';
    commentIdInput.value = `comment-${comment.commentId}`;

    newCommentElement.appendChild(commentNickname);
    newCommentElement.appendChild(commentContent);
    newCommentElement.appendChild(commentIdInput);

    /* 보여주는 순서 */
    if (isList) {
        commentElements.appendChild(newCommentElement);
    } else {
        commentElements.insertBefore(newCommentElement, commentElements.firstChild);
    }
}


/**
 * 댓글 리스트에 대한 권한을 확인한다.
 */
const getUserCommentId = () => {
    CommentApi.getUserCommentId(boardId).then((response) => {
        renderEditCommentArea(response);
    });
}

/**
 * 수정 폼을 그려준다. 
 */
const renderEditCommentForm = (commentId) => {
    const commentContent = document.querySelector(`input.comment-id[value="comment-${commentId}"]`)
                                    .closest('.comment-element, .reply-element')
                                    .querySelector('.comment-content');
    
    commentContent.innerHTML = `
                                <div class="comment-edit-area">
                                    <input type="text" class="comment-edit-input" placeholder="Comment">
                                    <button type="submit" class="btn btn-dark comment-edit-button">write</button>
                                </div>
                                `;
    const commentEditButtons = Array.from(document.getElementsByClassName('comment-edit-button'));

    commentEditButtons.forEach((commentEditButton) => {
        commentEditButton.addEventListener('click', () => {
            const parentCommentElement = commentEditButton.closest('.comment-element, .reply-element');
            const commentEditInput = parentCommentElement.querySelector('.comment-edit-input');
            const content = commentEditInput.value;

            updateComment(commentId, content);
        });
    });
}

/**
 * 댓글 수정을 요청한다.
 */
const updateComment = (commentId, content) => {
    CommentApi.updateComment(commentId, content);
    location.reload();
}

/**
 * 댓글 삭제를 요청한다.
 */
const deleteComment = (commentId) => {
    const deleteConfirm = window.confirm("삭제하시겠습니까?");
    if(deleteConfirm) {
        CommentApi.deleteComment(commentId);
    }
}

/**
 * 댓글 수정, 삭제 버튼을 보여준다.
 */
const renderEditCommentArea = (commentIdList) => {
    commentIdList.forEach((commentId) => {
        let userComment = document.querySelector(`input.comment-id[value="comment-${commentId}"]`);
    
        if (userComment) {
            const editAreaButtons = document.createElement('div');
            editAreaButtons.className = 'col-2 edit-area';
            editAreaButtons.innerHTML +=
                `
                <span class="edit-comment" id="edit-comment-${commentId}">수정</span>
                <span class="delete-comment" id="delete-comment-${commentId}">삭제</span>
                `;
            
            userComment.parentElement.appendChild(editAreaButtons);
            
            document.getElementById(`edit-comment-${commentId}`).addEventListener('click', () => {
                renderEditCommentForm(commentId);
            });

            document.getElementById(`delete-comment-${commentId}`).addEventListener('click', () => {
                deleteComment(commentId);
            });
        }
    });
}


export default {
    getCommentList,
    getUserCommentId
}
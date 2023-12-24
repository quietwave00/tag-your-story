import CommentApi from './commentApi.js';

/* 해당 스크립트는 board.html에서 함께 사용된다. */

const boardId = new URLSearchParams(window.location.search).get('boardId');
const pageSize = 20;
let currentPage = 1;
let endPage = 0;

window.addEventListener("load", () => {
    CommentApi.getCommentCountByBoardId(boardId)
        .then((response) => {
            const perPage = response.count / pageSize;
            (perPage < 1) ? endPage = 1 : endPage = Math.ceil(perPage);
            
            /**
             *  page-area에 대한 처리를 수행한다.
             */
            pagingCommentList();
        });
});


/**
 * 댓글 리스트를 요청한다.
 */
const getCommentList = (boardId) => {
    return CommentApi.getCommentList(boardId, currentPage)
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
const commentWriteButton = document.getElementById("comment-write-button");
if(commentWriteButton) {
    commentWriteButton.addEventListener("click", () => {
        const content = document.getElementById('comment-input').value;
        document.getElementById('comment-input').value = "";
        CommentApi.writeComment(boardId, content).then((response) => {
            renderComment(response, false, false);
        })
    });
}

/**
 * 댓글 리스트를 보여준다.
 */
const renderCommentList = (commentList) => {
    const commentElements = document.getElementById('comment-elements');
    commentElements.innerHTML = "";

    commentList.forEach(comment => {
        renderComment(comment.comment, true, false);
        if(comment.children.length > 0) {
            comment.children.slice(0, 5).forEach(child => {
                renderComment(child, true, true);
            });
            saveReplyList(comment.comment.commentId, comment.children, 1);
        }

        if(comment.children.length > 5) {
            const parentId = comment.comment.commentId;
            // if(!isLastReplyPage(parentId)) {
                renderAdditionalReplyListForm(parentId, comment.children[4].commentId);
            // }
        }
    });
}

/**
 *  답글이 마지막 페이지인지 여부를 돌려준다.
 * 
 * @param parentId: 답글의 부모 아이디
 */
const isLastReplyPage = (parentId) => {
    const replyListValue = replyListMap.get(parentId);
    let replyPage = replyListValue[1];
    return slicedReplyList.length < 6;
}

/**
 * 부모 댓글에 대한 답글 리스트를 저장해둔다.
 * value: 답글 리스트, 답글 페이지
 * @param parentId: 부모 아이디
 * @param replyList: 답글 리스트
 * @param replyPage: 답글 페이지
 */
const replyListMap = new Map();
const saveReplyList = (parentId, replyList, replyPage) => {
    let value = [replyList, replyPage];
    replyListMap.set(parentId, value);
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

    let replyButton;
    /* 답글 버튼 */
    if(!isReply) {
        replyButton = document.createElement('span');
        replyButton.className = 'reply-button';
        replyButton.textContent = '↳';

        replyButton.addEventListener('click', () => {
            renderReplyForm(comment.commentId);
        });
    }

    /* append */
    if (isReply) {
        const replySymbol = document.createTextNode('↳ ');
        commentContent.appendChild(replySymbol);
    }
    const textNode = document.createTextNode(comment.content);
    commentContent.appendChild(textNode);
    if(!isReply) {
        commentContent.appendChild(replyButton);
    }
    

    /* 댓글 아이디 */
    const commentIdInput = document.createElement('input');
    commentIdInput.type = 'hidden';
    commentIdInput.className = 'comment-id';
    commentIdInput.value = `comment-${comment.commentId}`;

    newCommentElement.appendChild(commentNickname);
    newCommentElement.appendChild(commentContent);
    newCommentElement.appendChild(commentIdInput);

    /* 보여주는 순서 */
    isList ?
        commentElements.appendChild(newCommentElement)
        : commentElements.insertBefore(newCommentElement, commentElements.firstChild);
}

/**
 * 답글 더보기 버튼을 그려준다.
 *
 * @param parentId: 답글의 부모 아이디 
 * @param lastReplyId: 화면에 보여지는 마지막 답글의 아이디
 */
const renderAdditionalReplyListForm = (parentId, lastReplyId) => {
    const comment = document.getElementById(`comment-${lastReplyId}`);
    const additionalReplyButton = document.createElement('span');
    additionalReplyButton.className = "more-replies";
    additionalReplyButton.textContent = "● ● ●";

    comment.insertAdjacentElement('afterend', additionalReplyButton);

    additionalReplyButton.addEventListener('click', () => {
        renderAdditionalReplyList(parentId, lastReplyId);
    });
}

/**
 * 답글을 추가로 보여준다.
 * 
 * @param parentId: 답글의 부모 아이디
 * @param lastReplyId: 화면에 보여지는 마지막 답글의 아이디
 */
const renderAdditionalReplyList = (parentId, lastReplyId) => {
    const lastReplyElement = document.getElementById(`comment-${lastReplyId}`);

    const replyListValue = replyListMap.get(parentId);
    const replyList = replyListValue[0];
    let replyPage = replyListValue[1];
    saveReplyList(parentId, replyList, ++replyPage);
    const slicedReplyList = replyList.slice(replyPage * 5 - 5, replyPage * 5);

    slicedReplyList.forEach(reply => {
        /* 답글 엘리먼트 */
        const replyElement = document.createElement('div');
        replyElement.className = 'row reply-element';
        replyElement.id = `comment-${reply.commentId}`;

        /* 닉네임 */
        const commentNickname = document.createElement('div');
        commentNickname.className = 'col-2 comment-nickname';
        commentNickname.textContent = reply.user.nickname;

        /* 내용 */
        const commentContent = document.createElement('div');
        commentContent.className = 'col-4 comment-content';
        const replySymbol = document.createTextNode('↳ ');
        commentContent.appendChild(replySymbol);

        const textNode = document.createTextNode(reply.content);
        commentContent.appendChild(textNode);

        replyElement.appendChild(commentNickname);
        replyElement.appendChild(commentContent);

        lastReplyElement.appendChild(replyElement);
    });
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

/**
 *  페이징 관련 함수
 */
const pagingCommentList = () => {
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

const onPageNumberClick = (page) => {
    CommentApi.getCommentList(boardId, page).then((response) => {
        renderCommentList(response);
    });
}


export default {
    getCommentList,
    getUserCommentId
}
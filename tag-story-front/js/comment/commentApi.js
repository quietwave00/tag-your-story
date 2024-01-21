import ExceptionHandler from '../global/exceptionHandler.js';

/**
 * 댓글 리스트를 요청한다.
 * 
 * @param boardId: 게시글 아이디 
 */
const getCommentList = (boardId, page) => {
    return fetch(`${server_host}/api/comments/${boardId}/${page - 1}`, {
        method: "GET"
    })
    .then((res) => res.json())
    .then(res => {
        if (res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    getCommentList(boardId);
                });
        }
    })
}

/**
 * 댓글 작성을 요청한다.
 * 
 * @param boardId: 게시글 아이디
 * @param content: 댓글 내용
 */
const writeComment = (boardId, content) => {
    return fetch(`${server_host}/api/comments`,{
        method:"POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        },
        body: JSON.stringify({
            "boardId": boardId,
            "content": content
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success == true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode);
        }
    });
}

/**
 * 댓글 리스트에 대한 권한 확인을 요청한다.
 * 
 * @param boardId: 게시글 아이디
 */
const getUserCommentId = (boardId) => {
    return fetch(`${server_host}/api/comments/auth/${boardId}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        }
    })
    .then((res) => res.json())
    .then(res => {
        if (res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode);
        }
    })
}

/**
 * 댓글 수정을 요청한다.
 * 
 * @param commentId: 댓글 아이디 
 * @param content: 댓글 내용
 */
const updateComment = (commentId, content) => {
    return fetch(`${server_host}/api/comments`,{
        method:"PATCH",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        },
        body: JSON.stringify({
            "commentId": commentId,
            "content": content
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success == true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode);
        }
    });
}

/**
 * 댓글 삭제를 요청한다.
 * 
 * @param commentId: 댓글 아이디
 */
const deleteComment = (commentId) => {
    fetch(`${server_host}/api/comments/status/${commentId}`,{
        method:"PATCH",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        }
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success == true) {
            alert("삭제되었습니다.");
            location.reload();
        } else {
            ExceptionHandler.handleException(res.exceptionCode);
        }
    });
}

/**
 * 답글 작성을 요청한다.
 * 
 * @param boardId: 게시글 아이디 
 * @param parentId: 부모 댓글 아이디
 * @param content: 답글 내용
 * @returns 
 */
const createReply = (boardId, parentId, content) => {
    return fetch(`${server_host}/api/comments/replies`,{
        method:"POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        },
        body: JSON.stringify({
            "boardId": boardId,
            "parentId": parentId,
            "content": content
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success == true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode);
        }
    });
}

/**
 * 게시글에 대한 전체 댓글 개수를 요청한다.
 * 
 * @param boardId: 게시글 아이디
 */
const getCommentCountByBoardId = (boardId) => {
    return fetch(`${server_host}/api/comments/count/${boardId}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json"
        }
    })
    .then((res) => res.json())
    .then(res => {
        if (res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode);
        }
    });
}

/**
 * 부모 댓글에 대한 답글 조회를 요청한다.
 * 
 * @param parentId: 부모 댓글 아이디
 * @param page: 페이지 수 
 */
const getReplyList = (parentId, page) => {
    return fetch(`${server_host}/api/comments/replies/${parentId}/${page}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json"
        }
    })
    .then((res) => res.json())
    .then(res => {
        if (res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode);
        }
    });
}



export default {
    getCommentList,
    writeComment,
    getUserCommentId,
    updateComment,
    deleteComment,
    createReply,
    getCommentCountByBoardId,
    getReplyList
}
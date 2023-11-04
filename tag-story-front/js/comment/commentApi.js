import ExceptionHandler from '../global/exceptionHandler.js';

/**
 * 댓글 리스트를 요청한다.
 * 
 * @param boardId: 게시글 아이디 
 */
const getCommentList = (boardId) => {
    return fetch(`${server_host}/api/comments/${boardId}`, {
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
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    writeComment(boardId, content);
                });
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
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    getUserCommentId(boardId);
                });
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
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    updateComment(commentId, content);
                });
        }
    });
}

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
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    deleteComment(commentId);
                });
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
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    createReply(boardId, parentId, content);
                });
        }
    });
}



export default {
    getCommentList,
    writeComment,
    getUserCommentId,
    updateComment,
    deleteComment,
    createReply
}
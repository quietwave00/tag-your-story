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

export default {
    getCommentList,
    writeComment
}
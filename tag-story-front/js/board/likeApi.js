import ExceptionHandler from '../global/exceptionHandler.js';

/**
 *  게시글에 좋아요를 요청한다.
 * 
 * @param boardId: 게시글 아이디
 */
const like = (boardId) => {
    fetch(`${server_host}/api/likes`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem("Authorization")
        },
        body: JSON.stringify({
            "boardId": boardId
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            document.getElementById('checkLiked').value = true;
            document.getElementById("like-button").innerHTML = "♥︎";
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() =>{
                    like(boardId);
                });
        }
    });
}

/**
 *  게시글에 좋아요 취소 요청한다.
 * 
 * @param boardId: 게시글 아이디
 */
const cancelLike = (boardId) => {
    fetch(`${server_host}/api/likes`, {
        method: 'DELETE',
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem("Authorization")
        },
        body: JSON.stringify({
            "boardId": boardId
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            document.getElementById('checkLiked').value = false;
            document.getElementById("like-button").innerHTML = "♡";
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() =>{
                    cancelLike(boardId);
                });
        }
    });
}

/**
 *  게시글의 좋아요 개수를 요청한다.
 * 
 * @param boardId: 게시글 아이디
 */
const getLikeCount = (boardId) => {
    return fetch(`${server_host}/api/likes/${boardId}`, {
        method: "GET"
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            return Promise.resolve(res.response)
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    getLikeCount(boardId);
                });
        }
    });
}

/**
 * 사용자의 좋아요 여부를 요청한다.
 * 
 * @param boardId: 게시글 아이디
 */
const checkLiked = (boardId) => {
    return fetch(`${server_host}/api/likes/status/${boardId}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem("Authorization")
        }
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            return Promise.resolve(res.response)
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    checkLiked(boardId);
                });
        }
    });
}

export default {
    like,
    cancelLike,
    getLikeCount,
    checkLiked
}

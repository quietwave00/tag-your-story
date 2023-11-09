import ExceptionHandler from '../global/exceptionHandler.js';

/**
 * 게시글 작성을 요청한다.
 * 
 * @param hashtagArray: 게시글에 포함된 해시태그 배열 
 * @param trackId: 게시글에 해당하는 트랙 아이디
 */
const writeBoard = (hashtagArray, trackId) => {
    return fetch(`${server_host}/api/boards`,{
        method:"POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        },
        body: JSON.stringify({
            "content": document.getElementById('board-input').value,
            "hashtagList": hashtagArray,
            "trackId": trackId
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success == true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    writeBoard(hashtagArray, trackId);
                });
        }
    });
}

/**
 * 트랙 아이디에 해당하는 게시물 리스트를 요청한다.
 * 
 * @param trackId: 트랙 아이디
 */
const getBoardListByTrackId = (trackId, page) => {
    return fetch(`${server_host}/api/boards/${trackId}/CREATED_AT?page=${page - 1}`, {
        method: "GET",
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            return Promise.resolve(res.response)
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    getBoardListByTrackId(trackId);
                });
        }
    });
}

/**
 * 게시글 아이디에 해당하는 상세 게시글 정보를 요청한다.
 * 
 * @param boardId: 게시글 아이디
 */
const getBoardByBoardId = (boardId) => {
    return fetch(`${server_host}/api/boards?boardId=${boardId}`, {
        method: "GET",
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            return Promise.resolve(res.response)
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    getBoardByBoardId(boardId);
                });
        }
    });
}

/**
 * 게시글의 작성자인지 확인을 요청한다.
 * 
 * @param boardId: 게시글 아이디
 */
const isWriter = (boardId) => {
    return fetch(`${server_host}/api/boards/auth/${boardId}`, {
        method:"GET",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        }
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            return Promise.resolve(res.response)
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    isWriter(boardId);
                });
        }
    });
}

/**
 *  게시글의 수정을 요청한다.
 * 
 */
const updateBoardAndHashtag = (boardId, content, hashtagArray) => {
    console.log("BoardApi: " + hashtagArray);
    return fetch(`${server_host}/api/boards`,{
        method:"PATCH",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        },
        body: JSON.stringify({
            "boardId": boardId,
            "content": content,
            "hashtagList": hashtagArray
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success == true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    updateBoardAndHashtag(boardId, content, hashtagArray);
                });
        }
    });
}

/**
 * 게시글 삭제를 요청한다.
 */
const deleteBoard = (boardId) => {
    fetch(`${server_host}/api/boards/${boardId}`,{
        method:"DELETE",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        }
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success == true) {
            alert("게시글이 삭제되었습니다.");
            window.history.back();
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    deleteBoard(boardId);
                });
        }
    });
}

/**
 * 해시태그에 해당하는 게시글 리스트를 요청한다.
 * 
 * @param hashtagName: 해시태그 이름
 */
const getBoardListByHashtagName = (hashtagName) => {
    return fetch(`${server_host}/api/boards/hashtags?hashtagName=${hashtagName}`, {
        method:"GET"
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    getBoardListByHashtagName(hashtagName);
                });
        }
    });
}


export default {
    writeBoard,
    getBoardListByTrackId,
    getBoardByBoardId,
    isWriter,
    updateBoardAndHashtag,
    deleteBoard,
    getBoardListByHashtagName
}
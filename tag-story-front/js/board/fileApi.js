import ExceptionHandler from '../global/exceptionHandler.js';

/**
 * 파일 업로드를 요청한다.
 */
const upload = (fileList,uploadFileRequest) => {
    return fetch(`${server_host}/api/files`, {
        method: "POST",
        headers: {
            "Authorization": localStorage.getItem('Authorization')
        },
        body: fileList
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
            .then(() => {
                upload(fileList, uploadFileRequest);
            });
        }
    });
}

/**
 * 파일 수정을 요청한다.
 */
const update = (fileList,uploadFileRequest) => {
    return fetch(`${server_host}/api/files`, {
        method: "PATCH",
        headers: {
            "Authorization": localStorage.getItem('Authorization')
        },
        body: fileList
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
            .then(() => {
                upload(fileList, uploadFileRequest);
            });
        }
    });
}

/**
 * 메인 이미지 파일 리스트를 요청한다.
 * 
 * @param trackId: 트랙 아이디
 */
const getMainFileList = (trackId, page) => {
    return fetch(`${server_host}/api/files/main/${trackId}?page=${page - 1}`, {
        method: "GET"
    })
    .then((res) => res.json())
    .then(res => {
        if (res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    getMainFileList(trackId);
                });
        }
    })
}

/**
 * 게시글의 파일 리스트를 요청한다.
 * 
 * @param boardId: 게시글 아이디
 */
const getFileList = (boardId) => {
    return fetch(`${server_host}/api/files/${boardId}`, {
        method: "GET"
    })
    .then((res) => res.json())
    .then(res => {
        if (res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    getFileList(boardId);
                });
        }
    })
}

/**
 * 파일 삭제 요청을 한다.
 * 
 * @param fileIdList: 삭제할 파일 아이디 리스트 
 */
const deleteFileList = (fileIdList, boardId) => {
    fetch(`${server_host}/api/files`, {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        },
        body: JSON.stringify({
            "fileIdList": fileIdList,
            "boardId": boardId
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === false) {
            ExceptionHandler.handleException(res.exceptionCode)
            .then(() => {
                deleteFileList(fileIdList);
            });
        }
    });
}


export default {
    upload,
    update,
    getMainFileList,
    getFileList,
    deleteFileList
}
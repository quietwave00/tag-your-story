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
 * 메인 이미지 파일 리스트를 요청한다.
 * 
 * @param trackId: 트랙 아이디
 */
const getMainFileList = (trackId) => {
    return fetch(`${server_host}/api/files/main/${trackId}`, {
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


export default {
    upload,
    getMainFileList,
    getFileList
}
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


export default {
    upload,
    getMainFileList
}
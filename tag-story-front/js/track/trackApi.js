import host from '../global/global.js';
import ExceptionHandler from '../global/exceptionHandler.js';

/**
 * 트랙 검색을 요청한다.
 * 
 * @param keyword: 트랙 검색 시 키워드 값 
 */
const searchTrack = (keyword, page) => {
    return fetch(`${host}/api/tracks?keyword=${keyword}&page=${page - 1}`, {
        method: "GET"
    })
    .then((res) => res.json())
    .then(res => {
        if (res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    searchTrack(keyword);
                });
        }
    })
}

export default {
    searchTrack
}
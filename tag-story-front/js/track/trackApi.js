import host from '../global/global.js';
import ExceptionHandler from '../global/exceptionHandler.js';

/**
 * 트랙 검색을 요청한다.
 * 
 * @param keyword: 트랙 검색 시 키워드 값 
 */
const searchTrack = (keyword, page) => {
    return fetch(`${server_host}/api/tracks?keyword=${keyword}&page=${page - 1}`, {
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

/**
 * 트랙의 상세 정보를 요청한다.
 * 
 * @param trackId: 트랙의 아이디 값
 * @returns 트랙의 상세 정보
 */
const getDetailTrackById = (trackId) => {
    return fetch(`${server_host}/api/tracks/${trackId}`, {
        method: "GET"
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            return Promise.resolve(res.response);
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
                .then(() => {
                    getDetailTrackById(trackId);
                });
        }
    });
}

export default {
    searchTrack,
    getDetailTrackById
}
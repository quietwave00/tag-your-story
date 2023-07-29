import host from '../global/global.js';
import ExceptionHandler from '../global/exceptionHandler.js';

/**
 * 로그아웃을 수행한다.
 */
const  logout = () => {
    console.log("로그아웃 실행됨");
    fetch(`${host}/api/logout`, {
        method: "POST",
        headers: {
            "Authorization": localStorage.getItem('Authorization')
        }
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            console.log("로그아웃성공");
            localStorage.removeItem('Authorization');
            localStorage.removeItem('RefreshToken');
            alert("로그아웃되었습니다.");
            window.location.href = 'http://localhost:5500/html/index.html';
        } else {
            console.log("로그아웃실패" + res.exceptionCode);
            ExceptionHandler.handleException(res.exceptionCode);
        }
    })
}

export default {
    logout
}
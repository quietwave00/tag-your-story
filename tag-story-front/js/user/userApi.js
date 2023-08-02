import host from '../global/global.js';
import ExceptionHandler from '../global/exceptionHandler.js';

/**
 * 로그아웃을 수행한다.
 */
const logout = () => {
    fetch(`${host}/api/logout`, {
        method: "POST",
        headers: {
            "Authorization": localStorage.getItem('Authorization')
        }
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            localStorage.removeItem('Authorization');
            localStorage.removeItem('RefreshToken');
            alert("로그아웃되었습니다.");
            window.location.href = 'http://localhost:5500/html/index.html';
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
            .then(() => {
                logout();
            });
        }
    });
}

/**
 * 로그인한 회원인지 회원가입한 회원인지 상태 체크를 요청한다.
 */
const checkRegisterUser = () => {
    fetch(`${host}/api/check-registration`, {
        method: "GET",
        headers: {
            "Authorization": localStorage.getItem('Authorization')
        }
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            if(res.response.registerUser === true) {
                window.location.href = 'http://localhost:5500/html/user/nickname.html';
            }
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
            .then(() => {
                checkRegisterUser();
            })
        }
    })
}

/**
 *  닉네임을 설정해준다.
 * 
 *  @param nickname : 변경할 닉네임 값
 */
const updateNickname = (nickname) => {
    console.log("updateNickname Called");
    fetch(`${host}/api/nicknames`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
            "Authorization": localStorage.getItem('Authorization')
        },
        body: JSON.stringify({
            "nickname": nickname
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            alert(`${res.response.nickname}님, 환영합니다!`);
            window.location.href = 'http://localhost:5500/html/index.html';
        } else {
            ExceptionHandler.handleException(res.exceptionCode)
            .then(() => {
                updateNickname();
            });
        }
    });
}

export default {
    logout,
    updateNickname,
    checkRegisterUser
}
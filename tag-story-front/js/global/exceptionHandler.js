/**
 * 반환받은 ExceptionCode에 따라 예외를 처리해준다.
 */
const handleError = (exceptionCode) => {
    switch (exceptionCode) {
        case 'TOKEN_HAS_EXPIRED':
            handleExpiredJwt();
            break;
    }
}


/**
 * 만료된 AccessToken으로 요청 시 실행된다.
 * 새로운 AccessToken을 설정한다.
 * @param _refreshToken : 리프레쉬 토큰
 */
const handleExpiredJwt = () => {
    fetch("http://localhost:8080/api/user/reissue/jwt", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            "refreshToken": localStorage.getItem('RefreshToken')
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            setJwt(res.response.newJwt);
        } else {
        
        }
    });
}
/**
 * 만료된 RefreshToken으로 요청 시 실행된다.
 * 새로운 RefreshToken을 설정한다.
 */
const handleExpiredRefreshToken = () => {
    fetch("http://localhost:8080/api/user/reissue/refreshToken", {
        method: "POST",
        headers: {
            "Authorization": localStorage.getItem('Authorization')
        },
        body: JSON.stringify({
            "refreshToken": localStorage.getItem('RefreshToken')
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.result === true) {
            setJwt(res.response.newJwt);
        } else {
        
        }
    });
}

/**
 *  발급받은 AccessToken을 설정한다.
 * 
 * @param newJwt : 서버로부터 발급받은 새로운 AccessToken
 */
const setJwt = (newJwt) => {
    localStorage.removeItem('Authorization');
    localStorage.setItem('Authorization', newJwt);
    console.log("newJwt: "+ newJwt);
}

export default {
    handleError
};
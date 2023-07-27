console.log("ExceptionHandler Execute");
function handleExpiredJwt(_refreshToken) {
    console.log("handleExpiredJwt Execute");
    fetch("http://localhost:8080/api/user/reissue/jwt", {
        method: "POST",
        body: JSON.stringify({
            "refreshToken": localStorage.getItem('RefreshToken')
        })
    })
    .then((res) => res.json())
    .then(res => {
        if(res.result === true) {
            setJwt(res.response.newJwt);
        } else {
            console.log(res.response);
        }
    });
}

function handleExpiredRefreshToken() {
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
            console.log(res.response);
        }
    });
}

function setJwt(newJwt) {
    localStorage.setItem('Authorization', newJwt);
}

export default {
    handleExpiredJwt,
    handleExpiredRefreshToken
};
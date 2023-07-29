import ExceptionHandler from './global/exceptionHandler.js';
import UserArea from './user/userArea.js';

window.onload = () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();
}

document.getElementById("test").addEventListener("click", function() {
    fetch("http://localhost:8080/api/test", {
        method: "GET",
        headers: {
            "Authorization": localStorage.getItem('Authorization')
        }
    })
    .then((res) => res.json())
    .then(res => {
        if(res.success === true) {
            console.log(res);
        } else {
            console.log(res);
            ExceptionHandler.handleError(res.exceptionCode);
        }
    })
})
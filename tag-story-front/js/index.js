import UserArea from './user/userArea.js';
import UserApi from './user/userApi.js';

window.onload = () => {
    /**
     * user-area에 대한 처리를 수행한다.
     */
    UserArea.setState();

    /**
     *  회원의 회원가입, 로그인 상태를 체크한다.
     */
    UserApi.checkRegisterUser();
}
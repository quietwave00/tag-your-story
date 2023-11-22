import UserApi from './userApi.js';

const nicknameAlert = document.getElementById('nickname-alert');

/**
 *  닉네임을 설정하는 Request 함수를 호출한다.
 */
document.getElementById('nickname-button').addEventListener('click', () => {
    const nickname = document.getElementById('nickname-input').value;
    nicknameAlert.innerText = "";
    if(validate(nickname)) {
        console.log(nickname);
        if(confirm(`${nickname}으로 가입하시겠습니까?`)) {
            UserApi.updateNickname();
        }
    }
});

/**
 * 닉네임 값을 검증한다.
 */
const validate = (nickname) => {
    if(!nickname) {
        nicknameAlert.innerText = "사용하실 닉네임을 입력해 주세요.";
        return false;
    }

    if (/\s/.test(nickname)) {
        nicknameAlert.innerText = "공백을 제거해 주세요.";
        document.getElementById('nickname-input').value = "";
        return false;
    }

    if (nickname.length > 7) {
        nicknameAlert.innerText = "7자를 초과할 수 없습니다.";
        document.getElementById('nickname-input').value = "";
        return false;
    }

    return true;
}



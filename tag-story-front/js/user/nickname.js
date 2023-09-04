import UserApi from 'https://d2lsho2su959kd.cloudfront.net/tag-story-front/js/user/userApi.js';

/**
 *  닉네임을 설정하는 Request 함수를 호출한다.
 */
document.querySelector('#nickname-button').addEventListener('click', function() {
    UserApi.updateNickname(document.querySelector('#nickname-input').value);
});
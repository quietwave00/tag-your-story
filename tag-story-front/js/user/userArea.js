import userApi from './userApi.js';

/**
 * user 상태에 따라 user-area의 역할을 변경해준다.
 */
const setState = () => {
const api = userApi.logout;


  console.log("setState() called");
    const jwt = localStorage.getItem('Authorization');
    const refreshToken = localStorage.getItem('RefreshToken');

    if(jwt == null && refreshToken == null) {
      console.log("로그인으로~");
      document.getElementById('user-area').innerHTML =
        `
        <a href = "http://localhost:5500/html/user/login.html">LOGIN</a>
        `;
    } else {
        document.getElementById('user-area').innerHTML = 
            `
            <div class="dropdown">
              <a href="#" class="d-block link-dark text-decoration-none dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
                <img src="../../image/user-image.png" alt="mdo" width="32" height="32" class="rounded-circle">
              </a>
              <ul class="dropdown-menu text-small">
                <li>
                  <a class="dropdown-item" href="#" id="test">Sign out</a>
                </li>
              </ul>
            </div>
            `;
    }

    document.getElementById('test').addEventListener ('click', function (){
      console.log('test clicked');
      userApi.logout();
    })
}

export default {
  setState
}

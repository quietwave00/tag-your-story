import UserApi from './userApi.js';

/**
 * user 상태에 따라 user-area의 역할을 변경해준다.
 */
const setState = () => {
    const jwt = localStorage.getItem('Authorization');
    const refreshToken = localStorage.getItem('RefreshToken');

    if(jwt == null && refreshToken == null) {
      document.getElementById('user-area').innerHTML =
        `
        <a href = '${client_host}/login.html'>LOGIN</a>
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
                  <a class="dropdown-item" href="#" id="logout-a">Sign out</a>
                </li>
              </ul>
            </div>
            `;
    }
    
    if(document.getElementById('logout-a') != null) {
      document.getElementById('logout-a').addEventListener ('click', function() {
        UserApi.logout();
      })
    }
}

const checkRegisterUser = () => {
  if(localStorage.getItem('Pending') != null) {
    window.location.href = `${client_host}/nickname.html`;
  }
}


export default {
  setState,
  checkRegisterUser
}

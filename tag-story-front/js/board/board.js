import BoardApi from "../board/boardApi.js"

/**
 * 해당 스크립트는 detail.html에서 details.js와 함께 사용된다. 
 */

const trackId = new URLSearchParams(window.location.search).get('trackId');
/**
 * 해시태그 입력 이벤트 함수
 */
const hashtagArray = [];
    let enterCount = 0;
    document.getElementById('tag-input').addEventListener('keypress', function(e) {
        if(e.key === "Enter") {
            enterCount++;
            if(enterCount > 5) {
                alert("해시태그는 다섯 개까지 입력 가능합니다.");
                this.value= "";
                e.preventDefault();
            } else {
                e.stopPropagation();
                e.preventDefault();
                const hashtag = this.value.trim();
                renderHashtag(hashtag);
                this.value = "";
            }
        }
    });

/**
 * 해시태그 요소를 보여준다.
 * @param hashtag: 해시태그 입력값
 */
const renderHashtag = (hashtag) => {
    document.getElementById('hashtag-container').innerHTML +=
        `
            <div class = "hashtag-elements" id = "tag-${enterCount}" style = "margin-right: 10px;" onclick = "deleteHashtag('tag-${enterCount}')">#${hashtag}</div>

        `;
    hashtagArray.push(hashtag);
}

/**
 * 해시태그 요소를 삭제한다.
 * @param hashtagId: 삭제할 해시태그 아이디
 */
const deleteHashtag = (hashtagId) => {
    document.getElementById(hashtagId).remove();
}

/**
 * 게시글 작성 버튼 클릭 시 이벤트 함수
 */
document.getElementById('write-button').addEventListener('click', () => {
    BoardApi.writeBoard(hashtagArray, trackId).then((response) => {renderBoard(response)});
    renderAlert();
});

/**
 * 게시글 작성 완료 알림을 보여준다.
 */
const renderAlert = () => {
    document.getElementById('alert-area').innerHTML =
            `
                <div class = "alert alert-dark hide" role = "alert">
                    Your board has been created successfully.
                </div>
            `;
            let alertDiv = document.querySelector('.alert');
            let opacity = 1;
            let timer = setInterval(function() {
                if (opacity > 0) {
                    opacity -= 0.005;
                    alertDiv.style.opacity = opacity;
                } else {
                    clearInterval(timer);
                    alertDiv.style.display = 'none';
                }
            }, 10);
}

/**
 * 게시글 작성을 요청하고 응답값을 토대로 렌더링해준다.
 */
const renderBoard = (board) => {
    console.log(JSON.stringify(board));
    let boardId = board.boardId;
    let hashtagList = board.hashtagList;
    let content = board.content;
    let tagElements = "";
    for(let hashtag of hashtagList) {
        tagElements += 
                `
                <div class = "tag-element">#${hashtag}</div>
                `;
    }
    const boardElementArea = document.getElementById('board-element-area');
    boardElementArea.insertAdjacentHTML('afterbegin', `
        <div class="col-5 board">
            <div class="row">
                <div>
                    <input type="hidden" class="board-id" value="${boardId}">
                    <div class="tag-area">${tagElements}</div>
                    <div class="content-area">${content}</div>
                </div>
            </div>
        </div>
        `);
        document.getElementById('hashtag-container').innerHTML = "";
        document.getElementById('board-input').value = "";

        moveDetails();
}

/**
 * 게시글의 상세 페이지로 이동한다.
 */
const moveDetails = () => {
    const boardElements = document.getElementsByClassName('board');

    for(let element of boardElements) {
        element.addEventListener('click', (e) => {
            const boardId = e.currentTarget.querySelector('.board-id').value;
            window.location.href = `board.html?boardId=${boardId}`;
        })
    }
}
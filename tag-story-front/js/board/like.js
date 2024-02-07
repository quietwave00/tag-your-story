import LikeApi from "./likeApi.js"

/**
 * 해당 스크립트는 board.html에서 board_detail.js와 함께 사용된다. 
 */
const boardId = new URLSearchParams(window.location.search).get('boardId');
const likeButton = document.getElementById('like-button');

/**
 * 사용자의 좋아요 여부를 검사한다.
 */
const checkLiked = (boardId) => {
    LikeApi.checkLiked(boardId).then((response) => {
        setStatus(response.liked); 
    });
}

/**
 * 좋아요 버튼 클릭 이벤트 함수
 */
let currentLikeCount = 0;
let debounceTime;

const likeButtonArea = document.getElementById('like-button-area');
if (likeButtonArea) {
    likeButtonArea.addEventListener('click', async () => {
        clearTimeout(debounceTime);

        debounceTime = setTimeout(async () => {
            const isLiked = likeButton.getAttribute('data-is-liked') === 'true';
            const likeApi = isLiked ? LikeApi.cancelLike : LikeApi.like;
            
            const result = await likeApi(boardId);
            
            if (result) {
                setStatus(!isLiked);
                currentLikeCount += isLiked ? -1 : 1;
                renderLikeCount(currentLikeCount);
            }
        }, 200);
    });
}
    
/**
 * 좋아요 개수를 보여준다.
 */
const renderLikeCount = (likeCount) => {
    currentLikeCount = likeCount;
    document.getElementById('like-alert-area').innerHTML = `<span id = "like-alert">${currentLikeCount} Like</span>`;
}

/**
 * 좋아요 여부에 따른 상태를 설정하고 보여준다.
 * @likeStatus : 좋아요 상태
 */
const setStatus = (likeStatus) => {
    likeButton.setAttribute('data-is-liked', likeStatus);
    likeStatus ? likeButton.innerHTML = "&#9829" : likeButton.innerHTML = "♡";
}


export default {
    checkLiked,
    renderLikeCount
}

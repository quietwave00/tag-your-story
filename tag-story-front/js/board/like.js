import LikeApi from "./likeApi.js"

/**
 * 해당 스크립트는 board.html에서 board_detail.js와 함께 사용된다. 
 */

document.getElementById('like-button-area').addEventListener('click', () => {
    if(document.getElementById('checkLiked').value === false) {
        LikeApi.like(boardId);
    } else {
        LikeApi.cancelLike(boardId);
    }
    /* 좋아요 개수를 요청한다 */
    LikeApi.getLikeCount(boardId).then((response) => {
        renderLikeCount(response);
    })
});

/**
 * 좋아요 개수를 보여준다.
 */
const renderLikeCount = (likeCount) => {
    document.getElementById('like-alert-area').innerHTML = `<span id = "like-alert">${likeCount} Like</span>`;
}

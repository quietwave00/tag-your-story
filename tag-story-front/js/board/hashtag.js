import BoardApi from './boardApi.js';


let hashtagArray = [];
let editFlag = false;
let hashtagCount = 0;

/**
 * 해시태그 입력 이벤트 함수
 */
const tagInput = document.getElementById('tag-input');
tagInput.addEventListener('keypress', function(e) {
    editFlag = true

    hashtagCount = hashtagArray.length;
    if(e.key === "Enter") {
        if(hashtagCount > 4) {
            alert("해시태그는 다섯 개까지 입력 가능합니다.");
            e.preventDefault();
        } else if(!tagInput.value) {
            alert("내용을 입력해 주세요.");
            e.preventDefault();
        } else {
            e.stopPropagation();
            e.preventDefault();
            const hashtag = this.value.trim();

            renderHashtag(hashtag, hashtagCount); 
        }
        this.value = "";
    }
});


/**
 * 해시태그 요소를 보여준다.
 * @param hashtag: 해시태그 입력값
 */
let hashtagElement = document.getElementById('hashtag-container');
const renderHashtag = (hashtag) => {
    hashtagCount++;
    hashtagElement.innerHTML +=
        `
            <div class = "hashtag-elements" id = "tag-${hashtagCount}" style = "margin-right: 10px;">#${hashtag}</div>

        `;
    hashtagArray.push(hashtag);
    hashtagCount = hashtagArray.length;
}


/**
 * 해시태그 요소를 삭제한다.
 * @param hashtagId: 삭제할 해시태그 아이디의 인덱스값
 */
hashtagElement.onclick = (e) => deleteHashtag(e.target.id);
const deleteHashtag = (hashtagId) => {
    editFlag = true;

    const element = document.getElementById(hashtagId);
    if (element) {
        element.remove();
        
        const prefix = "tag-";
        const index = hashtagId.replace(prefix, "") - 1;
        hashtagArray.splice(index, 1);
    }
    addIdToTagElements();
}


/**
 * 해시태그 요소들에 id를 부여해준다. (수정 시 사용)
 */
const addIdToTagElements = () => {
    const tagElements = document.querySelectorAll(".hashtag-elements");
    tagElements.forEach((tagElement, index) => {
        const tagId = `tag-${index + 1}`;
        tagElement.id = tagId;
    });
}

/**
 * hashtagArray에 기존 해시태그 값들을 넣어준다. (수정 시 사용)
 */
const addTagToHashtagArray = (existedHashtagArray) => {
    hashtagArray = [];
    existedHashtagArray.forEach(hashtag => {
        hashtagArray.push(hashtag);
    });
}

/**
 * hashtagArray를 초기화한다.
 */
const clearHashtagArray = () => {
    hashtagArray = [];
}

/**
 * 해시태그 클릭 시 해당 태그가 포함된 게시글을 보여준다.
 */
const getBoardListByHashtag = (hashtagName) => {
    BoardApi.getBoardListByHashtagName(hashtagName)
    .then((response) => {
        renderBoardListByHashtag(response);
    });
}

/**
 * 특정 해시태그가 포함된 게시글을 그려준다.
 */
const renderBoardListByHashtag = (boardList) => {
    document.getElementById('board-element-area').innerHTML = "";

    boardList.forEach(board => {
        document.getElementById('board-element-area').innerHTML += 
            `
            <div class = "board-element">
                <div class = "row">
                    <div>
                        <input type = "hidden" class = "board-id" value = "board-${board.boardId}">
                        <div class = "content-area">${board.content}</div>
                        <div class = "nickname-area">${board.nickname}</div>
                    </div>
                </div>
            </div>
            `;
    });
}

export { hashtagArray, editFlag };
export default {
    addIdToTagElements,
    addTagToHashtagArray,
    clearHashtagArray,
    getBoardListByHashtag
};
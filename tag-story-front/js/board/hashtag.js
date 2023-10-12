/**
 * 해시태그 입력 이벤트 함수
 */
let hashtagArray = [];
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
let hashtagElement = document.getElementById('hashtag-container');
const renderHashtag = (hashtag) => {
    hashtagElement.innerHTML +=
        `
            <div class = "hashtag-elements" id = "tag-${enterCount}" style = "margin-right: 10px;">#${hashtag}</div>

        `;
    hashtagArray.push(hashtag);
}

hashtagElement.onclick = (e) => deleteHashtag(e.target.id);
/**
 * 해시태그 요소를 삭제한다.
 * @param hashtagId: 삭제할 해시태그 아이디의 인덱스값
 */
const deleteHashtag = (hashtagId) => {
    document.getElementById(hashtagId).remove();
    const id = hashtagId.match(/-(\d+)/);
    const index = id[1] - 1;
    hashtagArray[index] = undefined;
}

/**
 * tag-elements 요소들에 id를 부여해준다.
 */
const addIdToTagElements = () => {
    const tagElements = document.querySelectorAll(".editable-tag-elements");
    tagElements.forEach((tagElement, index) => {
        const tagId = `tag-${index + 1}`;
        tagElement.id = tagId;
    });
};

/**
 * hashtagArray에 기존 해시태그 값들을 넣어준다. (수정 시 사용)
 */
const addTagToHashtagArrayForEditable = () => {
    const container = document.getElementById("hashtag-container");
    const tagElements = container.querySelectorAll(".editable-tag-elements");
    tagElements.forEach(tagElement => {
        const text = tagElement.textContent;
        const hashtag = text.substring(1);
        hashtagArray.push(hashtag);
    });
}

/**
 * hashtagArray에 기존 해시태그 값들을 넣어준다. (작성 시 사용)
 */
const addTagToHashtagArray = () => {
    const container = document.getElementById("hashtag-container");
    const tagElements = container.querySelectorAll(".editable-tag-elements");
    tagElements.forEach(tagElement => {
        const text = tagElement.textContent;
        const hashtag = text.substring(1);
        hashtagArray.push(hashtag);
    });
}


/**
 * 해시태그 클릭 시 해당 태그가 포함된 게시글을 보여준다.
 */
const getBoardListByHashtag = () => {}
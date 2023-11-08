
const existedHashtagElements = document.getElementsByClassName('hashtag-elements');
let hashtagArray = [];
let enterCount;
let editFlag = false; // 해시태그에 수정 사항이 생겼는지 판단하는 값
let editedHashtagArray = []; //수정 사항이 있을 때 모든 해시태그 값이 할당된다.



/**
 * 해시태그 입력 이벤트 함수
 */
document.getElementById('tag-input').addEventListener('keypress', function(e) {
    if(e.key === "Enter") {
        enterCount = existedHashtagElements ? existedHashtagElements.length : 0;
        enterCount++;
        console.log("enterCount: " + enterCount);
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
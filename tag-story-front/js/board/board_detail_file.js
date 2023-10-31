import FileApi from "../board/fileApi.js"

/* 해당 스크립트는 board.html에서 board_detail.js와 함께 사용된다. */ 

window.addEventListener("load", function () {
    const boardId = new URLSearchParams(window.location.search).get('boardId');

    /*
     * 상세 게시글의 파일 정보를 요청하고 렌더링한다.
     */
    FileApi.getFileList(boardId).then((response) => {
        renderFile(response);
    });
});

const fileArea = document.getElementById('file-area');
let fileId;
let filePath;
/**
 * 상세 게시글 정보를 보여준다. 
 */
const renderFile = (fileList) => {
    fileList.forEach(file => {
        const fileId = file.fileId;
        const filePath = file.filePath;

        const img = document.createElement('img');
        img.src = filePath;
        img.id = `file-${fileId}`;
        img.classList.add('file-element');

        fileArea.appendChild(img);
    });
}


/**
 * 수정 시 기존 데이터를 보여준다.
 */

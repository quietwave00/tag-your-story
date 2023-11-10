import FileApi from "../board/fileApi.js"
import { boardId as boardIdFrommModule } from "./edit.js";

/* 해당 스크립트는 board.html에서 board_detail.js와 함께 사용된다. */ 

window.addEventListener("load", function () {
    const boardIdFromURL = new URLSearchParams(window.location.search).get('boardId');
    const boardId = boardIdFromURL == null ? boardIdFrommModule : boardIdFromURL;
    /*
     * 상세 게시글의 파일 정보를 요청하고 렌더링한다.
     */
    FileApi.getFileList(boardId).then((response) => {
        this.document.getElementById('file-area') == null
            ? renderExistedFile(response) : renderFile(response);
    });
});

let fileList = new FormData();
let fileIdListToDelete = [];
let editFlag = false;

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

        const fileArea = document.getElementById('file-area');
        fileArea.appendChild(img);
    });
}


/**
 * 기존 데이터를 보여준다. (수정 시 사용)
 */
const renderExistedFile = (fileList) => {
    const uploadedView = document.getElementById('uploaded_view');
    fileList.forEach(file => {
        const fileId = file.fileId;
        const filePath = file.filePath;

        /* 미리보기에 추가 */
        let imgDiv = document.createElement("div");
        imgDiv.className = "existed_img_div";
        imgDiv.id = `existed-img${fileId}`;
        let img = document.createElement("img");
        img.src = filePath;
        imgDiv.appendChild(img);
        uploadedView.appendChild(imgDiv);
        uploadedView.classList.add("show");

        deleteExistedImg(fileId);
    });
}

/**
 * 업로드된 파일을 삭제한다. (수정 시 사용) 
 */
const deleteExistedImg = (fileId) => {
    const parentImgDiv = document.querySelector('#uploaded_view');
    const imgDivList = parentImgDiv.querySelectorAll('.existed_img_div');
    const btnOuter = document.querySelector(".button_outer");

    parentImgDiv.addEventListener('click', (event) => {
        event.stopPropagation();
    });

    imgDivList.forEach((imgDiv) => {
        imgDiv.addEventListener('click', (event) => {
            /* 미리보기에서 삭제 */
            event.currentTarget.remove();
            btnOuter.classList.remove("file_uploading");
            btnOuter.classList.remove("file_uploaded");
            
            /* 삭제할 파일 아이디 저장 */
            fileIdListToDelete.push(fileId);
        });
    });
}



/**
 * 파일 삭제, 수정 요청을 한다.
 */
document.getElementById("edit-button").addEventListener('click', () => {
    if(fileIdListToDelete.length > 0) {
        FileApi.deleteFileList(fileIdListToDelete);
    }
});

export { fileList, editFlag };
import File from "./file.js"
import FileApi from "./fileApi.js"
import { boardId as boardIdFromModule } from "./edit.js";

/* 해당 스크립트는 board.html에서 board_detail.js와 함께 사용된다. */ 

window.addEventListener("load", function () {
    const boardIdFromURL = new URLSearchParams(window.location.search).get('boardId');
    const boardId = boardIdFromURL == null ? boardIdFromModule : boardIdFromURL;
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
    });
    deleteExistedImg();
}

/**
 * 업로드된 파일을 삭제한다. (수정 시 사용) 
 */
const deleteExistedImg = () => {
    editFlag = true;

    const parentImgDiv = document.querySelector('#uploaded_view');
    const imgDivList = parentImgDiv.querySelectorAll('.existed_img_div');
    const btnOuter = document.querySelector(".button_outer");

    imgDivList.forEach((imgDiv) => {
        imgDiv.addEventListener('click', (event) => {
            /* 삭제할 파일 아이디 저장 */
            const imgDivId = event.currentTarget.id;
            saveDeleteFileId(imgDivId);

            /* 미리보기에서 삭제 */
            event.currentTarget.remove();
            btnOuter.classList.remove("file_uploaded");
        });
    });
}

/**
 * 삭제할 파일 아이디를 저장한다.
 */
const saveDeleteFileId = (imgDivId) => {
    const prefix = "existed-img";
    const fileId = imgDivId.replace(prefix, "");

    fileIdListToDelete.push(fileId);
}


/**
 * 파일 삭제, 수정 요청을 한다.
 */
const deleteAndUpdateFile = () => {
    console.log("deleteAndUpdateFile: " + fileIdListToDelete);
    if(fileIdListToDelete.length > 0) {
        console.log("여기안걸림?");
        FileApi.deleteFileList(fileIdListToDelete, boardIdFromModule);
    }
    File.update(boardIdFromModule);
}

export { fileList, editFlag };
export default {
    deleteAndUpdateFile
}
import FileApi from './fileApi.js';

/**
 * 해당 스크립트는 detail.html에서 detail.js와 함께 사용된다. 
 */

/**
 * 파일 이벤트
 */
const btnUpload = document.querySelector("#upload_file");
const btnOuter = document.querySelector(".button_outer");
const errorMsg = document.querySelector(".error_msg");
const uploadedView = document.querySelector("#uploaded_view");
const beforeFormData = new FormData();
let imgCount = 1;

/*
 *  이벤트에 따라 formData에 파일 정보를 담는다.
 */
btnUpload.addEventListener("change", function(e) {
    let ext = btnUpload.value.split('.').pop().toLowerCase();
    if (!['png', 'jpg', 'jpeg'].includes(ext)) {
        errorMsg.textContent = "이미지 파일을 선택해 주세요";
    } else {
        /* 이미지 미리보기 */
        btnOuter.classList.add("file_uploaded");
        let uploadedFile = URL.createObjectURL(e.target.files[0]);
        let imgDiv = document.createElement("div");
        imgDiv.className = "img_div"
        imgDiv.id = `img${imgCount++}`;
        let img = document.createElement("img");
        img.src = uploadedFile;
        imgDiv.appendChild(img);
        uploadedView.appendChild(imgDiv);
        uploadedView.classList.add("show");

        /* 파일을 formData에 추가 */
        const fileInput = e.target.files[0];
        beforeFormData.append(`imgDiv${imgDiv.id}`, fileInput);

        deleteImg();
    }
});

/*
 * 파일 삭제 함수
 */
const deleteImg = () => {
    const parentImgDiv = document.querySelector('#uploaded_view');
    const imgDivList = parentImgDiv.querySelectorAll('.img_div');

    parentImgDiv.addEventListener('click', (event) => {
        event.stopPropagation();
    });

    imgDivList.forEach((imgDiv) => {
        imgDiv.addEventListener('click', (event) => {
            /* 미리보기에서 삭제 */
            event.currentTarget.remove();
            btnOuter.classList.remove("file_uploading");
            btnOuter.classList.remove("file_uploaded");
            
            /* formData에서 삭제 */
            const fileKey = 'imgDiv' + imgDiv.id;
            beforeFormData.delete(fileKey);
        });
    });
}

/*
 * 파일 정보를 담아 FileApi로 요청한다.
 */
const upload = (boardId) => {
    const fileList = new FormData();
    for (const value of beforeFormData.values()) {
        fileList.append('fileList', value);
    }
    fileList.append('boardId', boardId);
    fileList.set('enctype', 'multipart/form-data');
    const uploadFileRequest = {
        boardId: boardId,
        fileList: fileList
    };
    return FileApi.upload(fileList, uploadFileRequest);
}

export default {
    upload
}

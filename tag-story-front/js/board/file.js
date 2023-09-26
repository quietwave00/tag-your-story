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

btnUpload.addEventListener("change", function(e) {
    let ext = btnUpload.value.split('.').pop().toLowerCase();
    if (!['png', 'jpg', 'jpeg'].includes(ext)) {
        errorMsg.textContent = "이미지 파일을 선택해 주세요";
    } else {
        //이미지 미리보기
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

        //formData에 추가
        const fileInput = e.target.files[0];
        beforeFormData.append(`imgDiv${imgDiv.id}`, fileInput);

        deleteImg();
    }
});

const deleteImg = () => {
    const parentImgDiv = document.querySelector('#uploaded_view');
    const imgDivList = parentImgDiv.querySelectorAll('.img_div');

    parentImgDiv.addEventListener('click', (event) => {
        event.stopPropagation();
    });

    imgDivList.forEach((imgDiv) => {
        imgDiv.addEventListener('click', (event) => {
            //미리보기에서 삭제
            event.currentTarget.remove();
            btnOuter.classList.remove("file_uploading");
            btnOuter.classList.remove("file_uploaded");
            
            //formData에서 삭제
            const fileKey = imgDiv.id;
            beforeFormData.delete(fileKey);
        });
    });
}

const upload = (boardId) => {
    const imageList = document.getElementsByClassName('img_div');
   // const beforeFormData2 = new FormData();
  //  beforeFormData2.append(`imgDiv${imgDiv.id}`, [...imageList]);
  //  console.log(imageList);
  //  console.log(beforeFormData2);
    const fileList = new FormData();
    // const uploadFileRequest = new FormData();
    for (const value of beforeFormData.values()) {
        console.log(value);
        fileList.append('fileList', value);
    }

   // fileList.append('fileList', [...imageList]);
    fileList.append('boardId', boardId);
    fileList.set('enctype', 'multipart/form-data');
    // console.log(fileList);
    const uploadFileRequest = {
        boardId: boardId
    };


  //  const uploadFileRequest = "";
 //   uploadFileRequest.append('uploadFileRequest', JSON.stringify(uploadFileRequestData));
    return FileApi.upload(fileList, uploadFileRequest);
}

export default {
    upload
}

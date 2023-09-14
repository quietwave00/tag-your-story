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
        //Image Preview
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

        //Add file to formData
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
            //Delete from Preview
            event.currentTarget.remove();
            btnOuter.classList.remove("file_uploading");
            btnOuter.classList.remove("file_uploaded");
            
            //Delete from formData
            const fileKey = imgDiv.id;
            console.log("fileKey: " + fileKey);
            beforeFormData.delete(fileKey);
        });
    });
}

const upload = (boardId) => {
    return FileApi.upload(beforeFormData, boardId);
}
    


export {
    upload
}

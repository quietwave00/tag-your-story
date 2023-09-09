package com.tagstory.api.domain.file;

import com.tagstory.api.domain.file.dto.request.UploadFileRequest;
import com.tagstory.api.domain.file.dto.response.UploadFileResponse;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.file.dto.response.UploadFile;
import com.tagstory.core.domain.file.service.FileFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class FileController {

    private final FileFacade fileFacade;

    /*
     * 파일을 게시한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping("/files")
    public ApiResult<List<UploadFileResponse>> upload(@RequestPart(value = "files") List<MultipartFile> fileList,
                                                      @RequestPart(value = "dto") UploadFileRequest uploadFileRequest) {
        List<UploadFile> uploadFileList = fileFacade.upload(fileList, uploadFileRequest.toCommand());
        return ApiUtils.success(uploadFileList.stream().map(UploadFileResponse::create).collect(Collectors.toList()));
    }

}

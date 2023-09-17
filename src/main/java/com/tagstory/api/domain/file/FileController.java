package com.tagstory.api.domain.file;

import com.tagstory.api.domain.file.dto.request.UploadFileRequest;
import com.tagstory.api.domain.file.dto.response.FileListResponse;
import com.tagstory.api.domain.file.dto.response.MainFileResponse;
import com.tagstory.api.domain.file.dto.response.UploadFileResponse;
import com.tagstory.api.utils.ApiUtils;
import com.tagstory.api.utils.dto.ApiResult;
import com.tagstory.core.domain.file.dto.response.FileList;
import com.tagstory.core.domain.file.dto.response.MainFile;
import com.tagstory.core.domain.file.dto.response.UploadFile;
import com.tagstory.core.domain.file.service.FileFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping( value = "/files")
    public ApiResult<List<UploadFileResponse>> upload(@ModelAttribute UploadFileRequest uploadFileRequest) {
        System.out.println(uploadFileRequest.getBoardId());
        List<UploadFile> uploadFileList = fileFacade.upload(uploadFileRequest.getFileList(), uploadFileRequest.toCommand());
        return ApiUtils.success(uploadFileList.stream().map(UploadFileResponse::from).collect(Collectors.toList()));
    }

    /*
     * 트랙 아이디에 해당하는 메인 이미지 파일을 조회한다.
     */
    @GetMapping("/files/{trackId}")
    public ApiResult<List<MainFileResponse>> getMainFileList(@PathVariable("trackId") String trackId) {
        List<MainFile> mainFileList = fileFacade.getMainFileList(trackId);
        return ApiUtils.success(mainFileList.stream().map(MainFileResponse::from).collect(Collectors.toList()));
    }

    /*
     * 게시물에 업로드된 파일 리스트를 조회한다.
     */
    @GetMapping("/files")
    public ApiResult<List<FileListResponse>> getFileList(@RequestParam("boardId") Long boardId) {
        List<FileList> fileList = fileFacade.getFileList(boardId);
        return ApiUtils.success(fileList.stream().map(FileListResponse::from).collect(Collectors.toList()));
    }
}

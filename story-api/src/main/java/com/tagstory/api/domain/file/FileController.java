package com.tagstory.api.domain.file;

import com.tagstory.api.domain.file.dto.request.DeleteFileRequest;
import com.tagstory.api.domain.file.dto.request.UploadFileRequest;
import com.tagstory.api.domain.file.dto.response.FileResponse;
import com.tagstory.api.domain.file.dto.response.MainFileResponse;
import com.tagstory.api.domain.file.dto.response.UploadFileResponse;
import com.tagstory.core.utils.ApiUtils;
import com.tagstory.core.utils.dto.ApiResult;
import com.tagstory.core.domain.file.dto.response.File;
import com.tagstory.core.domain.file.service.FileFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileFacade fileFacade;

    /*
     * 파일을 게시한다.
     */
    @PreAuthorize("hasRole('ROLE_USER')")
    @PostMapping
    public ApiResult<List<UploadFileResponse>> upload(@ModelAttribute UploadFileRequest uploadFileRequest) {
        List<File> response = fileFacade.upload(uploadFileRequest.getFileList(), uploadFileRequest.toCommand());
        return ApiUtils.success(response.stream().map(UploadFileResponse::from).collect(Collectors.toList()));
    }

    /*
     * 트랙 아이디에 해당하는 메인 이미지 파일을 조회한다.
     */
    @GetMapping("/main/{trackId}")
    public ApiResult<List<MainFileResponse>> getMainFileList(@PathVariable("trackId") String trackId) {
        List<File> response = fileFacade.getMainFileList(trackId);
        return ApiUtils.success(response.stream().map(MainFileResponse::from).collect(Collectors.toList()));
    }

    /*
     * 게시물에 업로드된 파일 리스트를 조회한다.
     */
    @GetMapping("/{boardId}")
    public ApiResult<List<FileResponse>> getFileList(@PathVariable("boardId") String boardId) {
        List<File> response = fileFacade.getFileList(boardId);
        return ApiUtils.success(response.stream().map(FileResponse::from).collect(Collectors.toList()));
    }

    /*
     * 파일을 삭제한다.
     * 레디스에 삭제할 파일의 정보를 저장하고 배치 작업으로 일괄 삭제 처리된다.
     */
    @DeleteMapping
    public ApiResult<Void> deleteFile(@RequestBody DeleteFileRequest request) {
        fileFacade.deleteFile(request.toCommand());
        return ApiUtils.success();
    }
}

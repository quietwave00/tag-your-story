package com.tagstory.api.domain.file.dto.response;

import com.tagstory.core.domain.file.dto.response.FileList;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileListResponse {
    private Long fileId;
    private String filePath;

    public static FileListResponse from(FileList fileList) {
        return FileListResponse.builder()
                .fileId(fileList.getFileId())
                .filePath(fileList.getFilePath())
                .build();
    }
}

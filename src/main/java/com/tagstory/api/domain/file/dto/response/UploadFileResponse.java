package com.tagstory.api.domain.file.dto.response;

import com.tagstory.core.domain.file.FileLevel;
import com.tagstory.core.domain.file.dto.response.UploadFile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadFileResponse {
    private Long fileId;
    private String filePath;
    private FileLevel fileLevel;

    public static UploadFileResponse create(UploadFile uploadFile) {
        return UploadFileResponse.builder()
                .fileId(uploadFile.getFileId())
                .filePath(uploadFile.getFilePath())
                .fileLevel(uploadFile.getFileLevel())
                .build();
    }
}

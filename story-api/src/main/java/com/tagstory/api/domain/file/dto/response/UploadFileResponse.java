package com.tagstory.api.domain.file.dto.response;

import com.tagstory.core.domain.file.FileLevel;
import com.tagstory.core.domain.file.dto.response.File;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadFileResponse {
    private Long fileId;
    private String filePath;
    private FileLevel fileLevel;

    public static UploadFileResponse from(File file) {
        return builder()
                .fileId(file.getFileId())
                .filePath(file.getFilePath())
                .fileLevel(file.getFileLevel())
                .build();
    }
}

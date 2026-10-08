package com.tagnote.api.domain.file.dto.response;

import com.tagnote.core.domain.file.FileLevel;
import com.tagnote.core.domain.file.service.File;
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

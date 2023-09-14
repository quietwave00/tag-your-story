package com.tagstory.core.domain.file.dto.response;

import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.file.FileLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadFile {
    private Long fileId;
    private String filePath;
    private FileLevel fileLevel;

    public static UploadFile onComplete(FileEntity file) {
        return UploadFile.builder()
                .fileId(file.getFileId())
                .filePath(file.getFilePath())
                .fileLevel(file.getFileLevel())
                .build();
    }
}

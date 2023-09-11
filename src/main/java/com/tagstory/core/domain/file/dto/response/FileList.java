package com.tagstory.core.domain.file.dto.response;

import com.tagstory.core.domain.file.FileEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileList {
    private Long fileId;
    private String filePath;

    public static FileList onComplete(FileEntity file) {
        return FileList.builder()
                .fileId(file.getFileId())
                .filePath(file.getFilePath())
                .build();
    }
}

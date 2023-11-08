package com.tagstory.core.domain.file.dto;

import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.file.FileLevel;
import com.tagstory.core.domain.file.FileStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class S3File {
    private int index;
    private String originalName;
    private String filePath;
    private FileLevel fileLevel;

    public FileEntity toEntity() {
        return FileEntity.builder()
                .fileName(this.getOriginalName())
                .filePath(this.getFilePath())
                .fileLevel(this.getFileLevel())
                .status(FileStatus.POST)
                .build();
    }

    public S3File addFileLevel(FileLevel fileLevel) {
        this.fileLevel = fileLevel;
        return this;
    }
}

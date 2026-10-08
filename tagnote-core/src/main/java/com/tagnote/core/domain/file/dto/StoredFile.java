package com.tagnote.core.domain.file.dto;

import com.tagnote.core.domain.file.FileEntity;
import com.tagnote.core.domain.file.FileLevel;
import com.tagnote.core.domain.file.FileStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoredFile {
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

    public StoredFile addFileLevel(FileLevel fileLevel) {
        this.fileLevel = fileLevel;
        return this;
    }

    public StoredFile setFileLevelToSub() {
        this.fileLevel = FileLevel.SUB;
        return this;
    }
}

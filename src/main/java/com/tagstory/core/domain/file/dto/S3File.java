package com.tagstory.core.domain.file.dto;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.file.FileLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class S3File {
    private int index;
    private String originalName;
    private String filePath;
    private FileLevel fileLevel;

    public FileEntity toEntity(BoardEntity board) {
        return FileEntity.builder()
                .fileName(originalName)
                .filePath(filePath)
                .fileLevel(fileLevel)
                .board(board)
                .build();
    }

    public S3File addFileLevel(FileLevel fileLevel) {
        this.fileLevel = fileLevel;
        return this;
    }
}

package com.tagnote.core.domain.file.service;

import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.file.FileEntity;
import com.tagnote.core.domain.file.FileLevel;
import com.tagnote.core.domain.file.FileStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class File {
    private Long fileId;

    private String fileName;

    private String filePath;

    private FileLevel fileLevel;

    private FileStatus status;

    private Board board;

    /*
     * 형변환
     */
    public FileEntity toEntity() {
        return FileEntity.builder()
                .fileId(this.getFileId())
                .fileName(this.getFileName())
                .filePath(this.getFilePath())
                .fileLevel(this.getFileLevel())
                .status(this.getStatus())
                .board(this.getBoard().toEntity())
                .build();
    }
}

package com.tagstory.core.domain.file.dto.response;

import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.file.FileLevel;
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
                .board(this.getBoard().toEntity())
                .build();
    }
}

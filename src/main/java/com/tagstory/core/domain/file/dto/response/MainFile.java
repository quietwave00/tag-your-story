package com.tagstory.core.domain.file.dto.response;

import com.tagstory.core.domain.file.FileEntity;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MainFile {
    private String boardId;
    private String filePath;

    public static MainFile onComplete(FileEntity file) {
        return MainFile.builder()
                .boardId(file.getBoard().getBoardId())
                .filePath(file.getFilePath())
                .build();
    }
}

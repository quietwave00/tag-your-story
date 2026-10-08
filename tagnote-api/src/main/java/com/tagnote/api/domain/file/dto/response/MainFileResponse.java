package com.tagnote.api.domain.file.dto.response;

import com.tagnote.core.domain.file.service.File;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MainFileResponse {
    private String boardId;
    private String filePath;

    public static MainFileResponse from(File file) {
        return builder()
                .boardId(file.getBoard().getBoardId())
                .filePath(file.getFilePath())
                .build();
    }
}

package com.tagstory.api.domain.file.dto.response;

import com.tagstory.core.domain.file.dto.response.MainFile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MainFileResponse {
    private Long boardId;
    private String filePath;

    public static MainFileResponse create(MainFile mainFile) {
        return MainFileResponse.builder()
                .boardId(mainFile.getBoardId())
                .filePath(mainFile.getFilePath())
                .build();
    }
}

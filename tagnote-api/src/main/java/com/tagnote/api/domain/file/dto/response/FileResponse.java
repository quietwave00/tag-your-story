package com.tagnote.api.domain.file.dto.response;

import com.tagnote.core.domain.file.service.File;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileResponse {
    private Long fileId;
    private String filePath;

    public static FileResponse from(File file) {
        return builder()
                .fileId(file.getFileId())
                .filePath(file.getFilePath())
                .build();
    }
}

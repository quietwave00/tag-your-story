package com.tagstory.api.domain.file.dto.request;

import com.tagstory.core.domain.file.dto.command.UploadFileCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileRequest {
    private Long boardId;

    public UploadFileCommand toCommand() {
        return UploadFileCommand.builder()
                .boardId(boardId)
                .build();
    }
}

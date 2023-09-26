package com.tagstory.core.domain.file.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadFileCommand {
    private String boardId;
}

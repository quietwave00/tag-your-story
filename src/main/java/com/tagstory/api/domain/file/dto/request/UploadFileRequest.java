package com.tagstory.api.domain.file.dto.request;

import com.tagstory.core.domain.file.dto.command.UploadFileCommand;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Builder
@Data
public class UploadFileRequest {
    private List<MultipartFile> fileList;
    private String boardId;

    public UploadFileCommand toCommand() {
        return UploadFileCommand.builder()
                .boardId(boardId)
                .build();
    }
}

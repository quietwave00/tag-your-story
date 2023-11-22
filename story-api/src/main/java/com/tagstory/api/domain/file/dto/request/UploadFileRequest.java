package com.tagstory.api.domain.file.dto.request;

import com.tagstory.core.domain.file.dto.command.UploadFileCommand;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Builder
@Data
public class UploadFileRequest {
    @NotEmpty(message = "파일 리스트는 비어 있을 수 없습니다.")
    private List<MultipartFile> fileList;

    @NotBlank(message = "boardId는 비어 있을 수 없습니다.")
    private String boardId;

    public UploadFileCommand toCommand() {
        return UploadFileCommand.builder()
                .boardId(boardId)
                .build();
    }
}

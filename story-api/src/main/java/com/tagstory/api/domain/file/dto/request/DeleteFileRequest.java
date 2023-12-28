package com.tagstory.api.domain.file.dto.request;

import com.tagstory.core.domain.file.dto.command.DeleteFileCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteFileRequest {
    @NotBlank(message = "boardId는 비어 있을 수 없습니다.")
    private String boardId;

    @NotEmpty(message = "fileIdList는 비어 있을 수 없습니다.")
    private List<Long> fileIdList;

    public DeleteFileCommand toCommand() {
        return DeleteFileCommand.builder()
                .boardId(boardId)
                .fileIdList(fileIdList)
                .build();
    }
}

package com.tagstory.api.domain.file.dto.request;

import com.tagstory.core.domain.file.dto.command.DeleteFileCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteFileRequest {
    List<Long> fileIdList;

    public DeleteFileCommand toCommand() {
        return DeleteFileCommand.builder()
                .fileIdList(fileIdList)
                .build();
    }
}

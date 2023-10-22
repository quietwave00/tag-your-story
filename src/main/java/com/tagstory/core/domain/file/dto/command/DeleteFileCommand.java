package com.tagstory.core.domain.file.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DeleteFileCommand {
    List<Long> fileIdList;


}

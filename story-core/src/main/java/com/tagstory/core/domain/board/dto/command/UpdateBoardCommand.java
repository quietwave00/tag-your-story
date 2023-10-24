package com.tagstory.core.domain.board.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UpdateBoardCommand {
    private String boardId;
    private String content;
    private List<String> hashtagList;
}

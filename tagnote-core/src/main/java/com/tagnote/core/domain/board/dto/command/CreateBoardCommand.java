package com.tagnote.core.domain.board.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreateBoardCommand {
    private String content;
    private String trackId;
    private List<String> userTagList;
    private Long userId;
}

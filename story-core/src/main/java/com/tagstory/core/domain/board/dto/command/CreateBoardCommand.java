package com.tagstory.core.domain.board.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreateBoardCommand {
    private String content;
    private String trackId;
    private List<String> hashtagList;
    private Long userId;
}

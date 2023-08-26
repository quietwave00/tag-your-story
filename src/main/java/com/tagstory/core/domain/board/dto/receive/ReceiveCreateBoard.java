package com.tagstory.core.domain.board.dto.receive;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReceiveCreateBoard {
    private String content;
    private String trackId;
    private List<String> hashtagList;
}

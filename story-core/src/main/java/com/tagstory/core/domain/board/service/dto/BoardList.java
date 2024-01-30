package com.tagstory.core.domain.board.service.dto;

import com.tagstory.core.domain.board.service.Board;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BoardList {
    private List<Board> boardList;
    private long totalCount;

    public static BoardList of(List<Board> boardList, long totalCount) {
        return BoardList.builder()
                .boardList(boardList)
                .totalCount(totalCount)
                .build();
    }
}

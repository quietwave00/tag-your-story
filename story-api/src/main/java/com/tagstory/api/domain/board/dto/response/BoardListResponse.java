package com.tagstory.api.domain.board.dto.response;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.tagstory.core.domain.board.service.dto.BoardList;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BoardListResponse {
    private List<BoardResponse> boardResponseList;
    private long totalCount;

    public static BoardListResponse from(BoardList boardList) {
        return BoardListResponse.builder()
                .boardResponseList(boardList.getBoardList().stream().map(BoardResponse::from).toList())
                .totalCount(boardList.getTotalCount())
                .build();
    }
}

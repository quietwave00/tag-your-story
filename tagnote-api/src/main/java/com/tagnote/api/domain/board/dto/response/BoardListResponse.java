package com.tagnote.api.domain.board.dto.response;

import com.tagnote.application.board.model.BoardPage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BoardListResponse {
    private List<BoardResponse> boardResponseList;
    private long totalCount;

    public static BoardListResponse from(BoardPage boardList) {
        return BoardListResponse.builder()
                .boardResponseList(boardList.boards().stream().map(BoardResponse::from).toList())
                .totalCount(boardList.totalCount())
                .build();
    }
}

package com.tagstory.core.domain.board.dto.response;

import com.tagstory.core.domain.board.Board;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardByTrackResponse {
    private Long boardId;
    private String content;
    private LocalDateTime createdDate;
    private String nickname;

    public static BoardByTrackResponse onComplete(Board board) {
        return BoardByTrackResponse.builder()
                .boardId(board.getBoardId())
                .content(board.getContent())
                .createdDate(board.getCreatedAt())
                .nickname(board.getUser().getNickname())
                .build();
    }
}

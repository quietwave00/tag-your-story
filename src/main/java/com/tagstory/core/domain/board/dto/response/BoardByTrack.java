package com.tagstory.core.domain.board.dto.response;

import com.tagstory.core.domain.board.BoardEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardByTrack {
    private Long boardId;
    private String content;
    private LocalDateTime createdAt;
    private String nickname;

    public static BoardByTrack onComplete(BoardEntity board) {
        return BoardByTrack.builder()
                .boardId(board.getBoardId())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .nickname(board.getUser().getNickname())
                .build();
    }
}

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

    public static BoardByTrack onComplete(BoardEntity boardEntity) {
        return BoardByTrack.builder()
                .boardId(boardEntity.getBoardId())
                .content(boardEntity.getContent())
                .createdAt(boardEntity.getCreatedAt())
                .nickname(boardEntity.getUserEntity().getNickname())
                .build();
    }
}

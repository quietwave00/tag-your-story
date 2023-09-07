package com.tagstory.api.domain.board.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardByTrackResponse {
    private Long boardId;
    private String content;
    private LocalDateTime createdAt;
    private String nickname;

    public static BoardByTrackResponse create(com.tagstory.core.domain.board.dto.response.BoardByTrack boardByTrack) {
        return BoardByTrackResponse.builder()
                .boardId(boardByTrack.getBoardId())
                .content(boardByTrack.getContent())
                .createdAt(boardByTrack.getCreatedAt())
                .nickname(boardByTrack.getNickname())
                .build();
    }

}

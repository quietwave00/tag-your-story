package com.tagstory.api.domain.board.dto.response;

import com.tagstory.core.domain.board.dto.response.BoardByTrackResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardByTrack {
    private Long boardId;
    private String content;
    private LocalDateTime createdDate;
    private String nickname;

    public static BoardByTrack create(BoardByTrackResponse boardByTrackResponse) {
        return BoardByTrack.builder()
                .boardId(boardByTrackResponse.getBoardId())
                .content(boardByTrackResponse.getContent())
                .createdDate(boardByTrackResponse.getCreatedDate())
                .nickname(boardByTrackResponse.getNickname())
                .build();
    }

}

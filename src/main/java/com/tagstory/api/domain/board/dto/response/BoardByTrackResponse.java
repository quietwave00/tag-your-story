package com.tagstory.api.domain.board.dto.response;

import com.tagstory.core.domain.board.dto.response.BoardByTrack;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BoardByTrackResponse {
    private String boardId;
    private String content;
    private LocalDateTime createdAt;
    private String nickname;
    private List<String> hashtagList;

    public static BoardByTrackResponse from(BoardByTrack boardByTrack) {
        return BoardByTrackResponse.builder()
                .boardId(boardByTrack.getBoardId())
                .content(boardByTrack.getContent())
                .createdAt(boardByTrack.getCreatedAt())
                .nickname(boardByTrack.getNickname())
                .hashtagList(boardByTrack.getHashtagList())
                .build();
    }

}

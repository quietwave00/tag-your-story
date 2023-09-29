package com.tagstory.api.domain.board.dto.response;

import com.tagstory.core.domain.board.dto.response.BoardByTrack;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardByTrackResponse {
    private String boardId;
    private String content;
    private LocalDateTime createdAt;
    private String nickname;
    private HashtagNameList hashtagNameList;

    public static BoardByTrackResponse from(BoardByTrack boardByTrack) {
        return BoardByTrackResponse.builder()
                .boardId(boardByTrack.getBoardId())
                .content(boardByTrack.getContent())
                .createdAt(boardByTrack.getCreatedAt())
                .nickname(boardByTrack.getNickname())
                .hashtagNameList(boardByTrack.getHashtagNameList())
                .build();
    }

}

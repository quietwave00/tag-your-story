package com.tagstory.api.domain.board.dto.response;

import com.tagstory.core.domain.board.dto.response.Board;
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

    public static BoardByTrackResponse from(Board board) {
        return BoardByTrackResponse.builder()
                .boardId(board.getBoardId())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .nickname(board.getUser().getNickname())
                .hashtagNameList(board.getHashtagNameList())
                .build();
    }
}

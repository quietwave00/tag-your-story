package com.tagstory.api.domain.board.dto.response;

import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DetailBoardResponse {
    private String content;
    private String nickname;
    private LocalDateTime createdAt;
    private HashtagNameList hashtagNameList;

    public static DetailBoardResponse from(Board board) {
        return DetailBoardResponse.builder()
                .content(board.getContent())
                .nickname(board.getUser().getNickname())
                .createdAt(board.getCreatedAt())
                .hashtagNameList(board.getHashtagNameList())
                .build();
    }
}

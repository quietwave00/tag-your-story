package com.tagstory.api.domain.board.dto.response;

import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNames;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardResponse {
    private String boardId;
    private String content;
    private LocalDateTime createdAt;
    private String nickname;
    private HashtagNames hashtagNameList;

    public static BoardResponse from(Board board) {
        return builder()
                .boardId(board.getBoardId())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .nickname(board.getUser().getNickname())
                .hashtagNameList(board.getHashtagNameList())
                .build();
    }
}

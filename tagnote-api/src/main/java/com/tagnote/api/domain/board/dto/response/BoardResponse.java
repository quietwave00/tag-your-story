package com.tagnote.api.domain.board.dto.response;

import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
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
    private UserTagNames userTagNameList;

    public static BoardResponse from(Board board) {
        return builder()
                .boardId(board.getBoardId())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .nickname(board.getUser().getNickname())
                .userTagNameList(board.getUserTagNameList())
                .build();
    }
}

package com.tagnote.api.domain.board.dto.response;

import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CreateBoardResponse {
    private String boardId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private UserTagNames userTagList;

    public static CreateBoardResponse from(Board board) {
        return builder()
                .boardId(board.getBoardId())
                .nickname(board.getUser().getNickname())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .userTagList(board.getUserTagNameList())
                .build();
    }
}

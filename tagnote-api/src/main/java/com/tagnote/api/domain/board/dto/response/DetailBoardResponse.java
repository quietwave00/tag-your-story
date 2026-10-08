package com.tagnote.api.domain.board.dto.response;

import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DetailBoardResponse {
    private String content;
    private String nickname;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private UserTagNames userTagNameList;

    public static DetailBoardResponse from(Board board) {
        return builder()
                .content(board.getContent())
                .nickname(board.getUser().getNickname())
                .likeCount(board.getLikeCount())
                .createdAt(board.getCreatedAt())
                .userTagNameList(board.getUserTagNameList())
                .build();
    }
}

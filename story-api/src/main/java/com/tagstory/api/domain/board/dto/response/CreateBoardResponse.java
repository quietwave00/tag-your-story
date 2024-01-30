package com.tagstory.api.domain.board.dto.response;

import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNames;
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
    private HashtagNames hashtagList;

    public static CreateBoardResponse from(Board board) {
        return builder()
                .boardId(board.getBoardId())
                .nickname(board.getUser().getNickname())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .hashtagList(board.getHashtagNameList())
                .build();
    }
}

package com.tagstory.api.domain.board.dto.response;

import com.tagstory.core.domain.board.dto.response.CreateBoard;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CreateBoardResponse {
    private Long boardId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private List<String> hashtagList;

    public static CreateBoardResponse from(CreateBoard createBoard) {
        return CreateBoardResponse.builder()
                .boardId(createBoard.getBoardId())
                .nickname(createBoard.getNickname())
                .content(createBoard.getContent())
                .createdAt(createBoard.getCreatedAt())
                .hashtagList(createBoard.getHashtagList())
                .build();
    }
}

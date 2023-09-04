package com.tagstory.api.domain.board.dto.response;

import com.tagstory.core.domain.board.dto.response.CreateBoardResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CreateBoard {
    private Long boardId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private List<String> hashtagList;

    public static CreateBoard create(CreateBoardResponse createBoardResponse) {
        return CreateBoard.builder()
                .boardId(createBoardResponse.getBoardId())
                .nickname(createBoardResponse.getNickname())
                .content(createBoardResponse.getContent())
                .createdAt(createBoardResponse.getCreatedAt())
                .hashtagList(createBoardResponse.getHashtagList())
                .build();
    }
}

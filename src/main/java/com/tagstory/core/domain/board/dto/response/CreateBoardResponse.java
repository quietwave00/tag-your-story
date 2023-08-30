package com.tagstory.core.domain.board.dto.response;

import com.tagstory.core.domain.board.Board;
import com.tagstory.core.domain.hashtag.Hashtag;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class CreateBoardResponse {
    private Long boardId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private List<String> hashtagList;

    public static CreateBoardResponse onComplete(Board board) {
        return CreateBoardResponse.builder()
                .boardId(board.getBoardId())
                .nickname(board.getUser().getNickname())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .hashtagList(board.getHashtagList().stream().map(Hashtag::getName).collect(Collectors.toList()))
                .build();
    }
}

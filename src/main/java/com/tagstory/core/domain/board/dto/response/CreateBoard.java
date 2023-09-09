package com.tagstory.core.domain.board.dto.response;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class CreateBoard {
    private Long boardId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private List<String> hashtagList;

    public static CreateBoard onComplete(BoardEntity board) {
        return CreateBoard.builder()
                .boardId(board.getBoardId())
                .nickname(board.getUser().getNickname())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .hashtagList(board.getHashtagList().stream().map(HashtagEntity::getName).collect(Collectors.toList()))
                .build();
    }
}

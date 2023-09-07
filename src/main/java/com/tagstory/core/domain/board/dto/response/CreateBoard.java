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

    public static CreateBoard onComplete(BoardEntity boardEntity) {
        return CreateBoard.builder()
                .boardId(boardEntity.getBoardId())
                .nickname(boardEntity.getUserEntity().getNickname())
                .content(boardEntity.getContent())
                .createdAt(boardEntity.getCreatedAt())
                .hashtagList(boardEntity.getHashtagEntityList().stream().map(HashtagEntity::getName).collect(Collectors.toList()))
                .build();
    }
}

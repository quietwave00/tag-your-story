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
public class CreateBoardResponse {
    private Long boardId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private List<String> hashtagList;

    public static CreateBoardResponse onComplete(BoardEntity boardEntity) {
        return CreateBoardResponse.builder()
                .boardId(boardEntity.getBoardId())
                .nickname(boardEntity.getUserEntity().getNickname())
                .content(boardEntity.getContent())
                .createdAt(boardEntity.getCreatedAt())
                .hashtagList(boardEntity.getHashtagEntityList().stream().map(HashtagEntity::getName).collect(Collectors.toList()))
                .build();
    }
}

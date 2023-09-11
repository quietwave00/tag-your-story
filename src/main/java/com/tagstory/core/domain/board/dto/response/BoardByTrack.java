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
public class BoardByTrack {
    private Long boardId;
    private String content;
    private LocalDateTime createdAt;
    private String nickname;
    private List<String> hashtagList;

    public static BoardByTrack onComplete(BoardEntity board) {
        return BoardByTrack.builder()
                .boardId(board.getBoardId())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .nickname(board.getUser().getNickname())
                .hashtagList(board.getHashtagList().stream().map(HashtagEntity::getName).collect(Collectors.toList()))
                .build();
    }
}

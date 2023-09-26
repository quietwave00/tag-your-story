package com.tagstory.core.domain.board.dto.response;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardByTrack {
    private String boardId;
    private String content;
    private LocalDateTime createdAt;
    private String nickname;
    private HashtagNameList hashtagNameList;


    public static BoardByTrack onComplete(BoardEntity board, HashtagNameList hashtagNameList) {
        return BoardByTrack.builder()
                .boardId(board.getBoardId())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .nickname(board.getUser().getNickname())
                .hashtagNameList(hashtagNameList)
                .build();
    }
}

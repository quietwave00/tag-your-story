package com.tagnote.api.domain.board.dto.response;

import com.tagnote.application.board.model.BoardView;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BoardResponse {
    private String boardId;
    private String content;
    private LocalDateTime createdAt;
    private String nickname;
    private UserTagNames userTagNameList;
    private String subjectType;
    private String trackId;
    private String albumId;
    private List<BoardSystemTagResponse> systemTags;

    public static BoardResponse from(BoardView board) {
        return builder()
                .boardId(board.boardId())
                .content(board.content())
                .createdAt(board.createdAt())
                .nickname(board.nickname())
                .userTagNameList(UserTagNames.ofNameList(board.userTagNames()))
                .subjectType(board.subject().type().name())
                .trackId(board.subject().type() == com.tagnote.domain.enrichment.subject.SubjectType.TRACK
                        ? board.subject().spotifyId() : null)
                .albumId(board.subject().type() == com.tagnote.domain.enrichment.subject.SubjectType.ALBUM
                        ? board.subject().spotifyId() : null)
                .systemTags(board.systemTags().stream().map(BoardSystemTagResponse::from).toList())
                .build();
    }
}

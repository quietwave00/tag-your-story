package com.tagnote.application.board.model;

import java.time.LocalDateTime;
import java.util.List;

public record BoardView(
        String boardId,
        String content,
        LocalDateTime createdAt,
        String nickname,
        Integer likeCount,
        BoardSubject subject,
        List<String> userTagNames,
        List<BoardSystemTag> systemTags
) {
    public BoardView {
        userTagNames = List.copyOf(userTagNames);
        systemTags = List.copyOf(systemTags);
    }
}

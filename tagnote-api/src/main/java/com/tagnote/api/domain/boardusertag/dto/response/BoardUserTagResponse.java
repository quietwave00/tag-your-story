package com.tagnote.api.domain.boardusertag.dto.response;

import com.tagnote.core.domain.boardusertag.service.BoardUserTag;
import com.tagnote.core.domain.usertag.service.UserTag;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardUserTagResponse {
    private String boardId;
    private UserTag userTag;

    public static BoardUserTagResponse from(BoardUserTag boardUserTag) {
        return BoardUserTagResponse.builder()
                .boardId(boardUserTag.getBoardId())
                .userTag(boardUserTag.getUserTag())
                .build();
    }
}

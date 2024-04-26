package com.tagstory.api.domain.boardhashtag.dto.response;

import com.tagstory.core.domain.boardhashtag.service.BoardHashtag;
import com.tagstory.core.domain.hashtag.service.Hashtag;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardHashtagResponse {
    private String boardId;
    private Hashtag hashtag;

    public static BoardHashtagResponse from(BoardHashtag boardHashtag) {
        return BoardHashtagResponse.builder()
                .boardId(boardHashtag.getBoardId())
                .hashtag(boardHashtag.getHashtag())
                .build();
    }
}

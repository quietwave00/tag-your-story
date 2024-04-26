package com.tagstory.core.domain.boardhashtag.service;

import com.tagstory.core.domain.hashtag.service.Hashtag;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BoardHashtag {

    private Long boardHashtagId;

    private String boardId;

    private Hashtag hashtag;
}

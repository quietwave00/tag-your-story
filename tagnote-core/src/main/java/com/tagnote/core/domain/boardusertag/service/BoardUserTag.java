package com.tagnote.core.domain.boardusertag.service;

import com.tagnote.core.domain.usertag.service.UserTag;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BoardUserTag {

    private Long boardUserTagId;

    private String boardId;

    private UserTag userTag;
}

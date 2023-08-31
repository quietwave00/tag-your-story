package com.tagstory.api.domain.board.dto.request;

import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreateBoardRequest {
    private String content;
    private String trackId;
    private List<String> hashtagList;

    public CreateBoardCommand toCommand(Long userId) {
        return CreateBoardCommand.builder()
                .content(content)
                .trackId(trackId)
                .hashtagList(hashtagList)
                .userId(userId)
                .build();
    }
}

package com.tagstory.api.domain.board.dto.request;

import com.tagstory.core.domain.board.dto.receive.ReceiveCreateBoard;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CreateBoardRequest {
    private String content;
    private String trackId;
    private List<String> hashtagList;

    public ReceiveCreateBoard toCommand() {
        return ReceiveCreateBoard.builder()
                .content(content)
                .trackId(trackId)
                .hashtagList(hashtagList)
                .build();
    }
}

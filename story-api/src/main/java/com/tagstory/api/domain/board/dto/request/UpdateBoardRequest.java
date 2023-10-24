package com.tagstory.api.domain.board.dto.request;

import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.command.UpdateBoardCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBoardRequest {
    private String boardId;
    private String content;
    private List<String> hashtagList;

    public UpdateBoardCommand toCommand() {
        return UpdateBoardCommand.builder()
                .boardId(boardId)
                .content(content)
                .hashtagList(hashtagList)
                .build();
    }
}

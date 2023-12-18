package com.tagstory.api.domain.board.dto.request;

import com.tagstory.core.domain.board.dto.command.UpdateBoardCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBoardRequest {

    @NotNull(message = "boardId는 비어 있을 수 없습니다.")
    private String boardId;

    @NotNull(message = "content는 비어 있을 수 없습니다.")
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

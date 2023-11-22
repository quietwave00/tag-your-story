package com.tagstory.api.domain.board.dto.request;

import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardRequest {
    @NotNull(message = "content는 비어 있을 수 없습니다.")
    private String content;

    @NotBlank(message = "trackId는 비어 있을 수 없습니다.")
    private String trackId;

    @NotEmpty(message = "해시태그는 비어 있을 수 없습니다.")
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

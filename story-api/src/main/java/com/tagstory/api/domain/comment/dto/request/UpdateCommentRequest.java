package com.tagstory.api.domain.comment.dto.request;

import com.tagstory.core.domain.comment.service.dto.command.UpdateCommentCommand;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentRequest {
    private Long commentId;
    private String content;

    public UpdateCommentCommand toCommand() {
        return UpdateCommentCommand.builder()
                .commentId(commentId)
                .content(content)
                .build();
    }
}

package com.tagnote.core.domain.comment.service;

import com.tagnote.core.domain.comment.service.dto.command.UpdateCommentCommand;
import com.tagnote.core.domain.comment.service.dto.response.CommentWithReplies;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CommentFacade {
    private final CommentService commentService;

    public Comment update(UpdateCommentCommand command) {
        return commentService.update(command);
    }

    public void delete(Long commentId) {
        commentService.delete(commentId);
    }

    public List<CommentWithReplies> getCommentList(String boardId, int page) {
        return commentService.getCommentList(boardId, page);
    }

    public List<Long> getUserCommentId(String boardId, Long userId) {
        return commentService.getUserCommentId(boardId, userId);
    }

    public int getCommentCountByBoardId(String boardId) {
        return commentService.getCommentCountByBoardId(boardId);
    }

    public List<Comment> getReplyList(Long parentId, int page) {
        return commentService.getReplyList(parentId, page);
    }
}

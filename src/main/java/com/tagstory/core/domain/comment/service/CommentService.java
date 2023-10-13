package com.tagstory.core.domain.comment.service;

import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.comment.CommentEntity;
import com.tagstory.core.domain.comment.repository.CommentRepository;
import com.tagstory.core.domain.comment.service.dto.Comment;
import com.tagstory.core.domain.comment.service.dto.command.CreateCommentCommand;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;

    @Transactional
    public Comment create(Board board, User user, CreateCommentCommand command) {
        CommentEntity commentEntity = CommentEntity.create(command);
        commentEntity.addUser(user.toEntity());
        commentEntity.addBoard(board.toEntity());

        return commentRepository.save(commentEntity).toComment();
    }
}

package com.tagstory.core.domain.like.service;

import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.like.dto.command.CancelLikeCommand;
import com.tagstory.core.domain.like.dto.command.LikeBoardCommand;
import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LikeFacade {
    private final LikeService likeService;
    private final BoardService boardService;
    private final UserService userService;

    public void like(LikeBoardCommand command) {
        boardService.increaseLikeCount(command.getBoardId());

        Board board = boardService.getBoardByBoardId(command.getBoardId());
        User user = userService.getCacheByUserId(command.getUserId());
        likeService.like(board, user);
    }

    public void cancelLike(CancelLikeCommand command) {
        boardService.decreaseLikeCount(command.getBoardId());

        Board board = boardService.getBoardByBoardId(command.getBoardId());
        User user = userService.getCacheByUserId(command.getUserId());
        likeService.cancelLike(board, user);
    }

    public boolean isLiked(String boardId, Long userId) {
        return likeService.isLiked(boardId, userId);
    }
}

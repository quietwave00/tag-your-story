package com.tagstory.core.domain.like.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.like.dto.command.CancelLikeCommand;
import com.tagstory.core.domain.like.dto.command.LikeBoardCommand;
import com.tagstory.core.domain.like.dto.response.LikeCount;
import com.tagstory.core.domain.user.service.dto.response.User;
import com.tagstory.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LikeFacade {
    private final LikeService likeService;
    private final BoardService boardService;
    private final UserService userService;

    public void like(LikeBoardCommand likeBoardCommand) {
        BoardEntity board = boardService.findByBoardId(likeBoardCommand.getBoardId());
        User user = userService.getCacheByUserId(likeBoardCommand.getUserId());
        likeService.like(board, user);
    }

    public LikeCount getLikeCount(String boardId) {
        BoardEntity board = boardService.findByBoardId(boardId);
        return likeService.getLikeCount(board);
    }

    public void cancelLike(CancelLikeCommand cancelLikeCommand) {
        BoardEntity board = boardService.findByBoardId(cancelLikeCommand.getBoardId());
        User user = userService.getCacheByUserId(cancelLikeCommand.getUserId());
        likeService.cancelLike(board, user);
    }
}

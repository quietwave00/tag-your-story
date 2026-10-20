package com.tagnote.core.domain.like.service;

import com.tagnote.application.user.ActiveUserWriteGuard;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.board.service.BoardService;
import com.tagnote.core.domain.like.dto.command.LikeBoardCommand;
import com.tagnote.core.domain.like.dto.command.UnLikeCommand;
import com.tagnote.core.domain.user.service.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeFacade {
    private final LikeService likeService;
    private final BoardService boardService;
    private final ActiveUserWriteGuard activeUserWriteGuard;

    @Transactional
    public Boolean like(LikeBoardCommand command) {
        User user = activeUserWriteGuard.lock(command.getUserId()).toUser();
        boardService.increaseLikeCount(command.getBoardId());
        Board board = boardService.getBoardByBoardId(command.getBoardId());
        return likeService.like(board, user);
    }

    @Transactional
    public void unLike(UnLikeCommand command) {
        activeUserWriteGuard.lock(command.getUserId());
        boardService.decreaseLikeCount(command.getBoardId());
        likeService.unLike(command.getBoardId(), command.getUserId());
    }

    public boolean isLiked(String boardId, Long userId) {
        return likeService.isLiked(boardId, userId);
    }
}

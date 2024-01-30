package com.tagstory.core.domain.like.service;

import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.like.dto.command.LikeBoardCommand;
import com.tagstory.core.domain.like.dto.command.UnLikeCommand;
import com.tagstory.core.domain.user.service.User;
import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.utils.lock.LockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LikeFacade {
    private final LikeService likeService;
    private final BoardService boardService;
    private final UserService userService;

    private final LockManager lockManager;

    public void like(LikeBoardCommand command) {
        /* api 중복 요청 방지를 위한 분산 락 적용 */
        lockManager.lock(Like.getLockNameOfKey(command.getUserId(), command.getBoardId()));

        boardService.increaseLikeCount(command.getBoardId());
        Board board = boardService.getBoardByBoardId(command.getBoardId());
        User user = userService.getCacheByUserId(command.getUserId());

        likeService.like(board, user);
    }

    @Transactional
    public void unLike(UnLikeCommand command) {
        boardService.decreaseLikeCount(command.getBoardId());

        /* 데이터 정합성을 위하여 대상 Like 객체에 락을 건다. */
        likeService.findByBoardIdAndUserId(command.getBoardId(), command.getUserId());
//        lockManager.lock(Like.getLockNameOfKey(like.getLikeId()));

        likeService.unLike(command.getBoardId(), command.getUserId());
    }

    public boolean isLiked(String boardId, Long userId) {
        return likeService.isLiked(boardId, userId);
    }
}

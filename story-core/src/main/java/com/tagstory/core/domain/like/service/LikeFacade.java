package com.tagstory.core.domain.like.service;

import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.like.LikeEntity;
import com.tagstory.core.domain.like.dto.command.LikeBoardCommand;
import com.tagstory.core.domain.like.dto.command.UnLikeCommand;
import com.tagstory.core.domain.like.repository.LikeRepository;
import com.tagstory.core.domain.user.service.User;
import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.utils.lock.LockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeFacade {
    private final LikeService likeService;
    private final BoardService boardService;
    private final UserService userService;
    private final LikeRepository likeRepository;

    public Boolean like(LikeBoardCommand command) {
        boardService.increaseLikeCount(command.getBoardId());
        Board board = boardService.getBoardByBoardId(command.getBoardId());
        User user = userService.getCacheByUserId(command.getUserId());
        return likeService.like(board, user);
    }

    @Transactional
    public void unLike(UnLikeCommand command) {
        boardService.decreaseLikeCount(command.getBoardId());
        likeService.unLike(command.getBoardId(), command.getUserId());
    }

    public boolean isLiked(String boardId, Long userId) {
        return likeService.isLiked(boardId, userId);
    }
}

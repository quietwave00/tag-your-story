package com.tagstory.api.aop;

import com.tagstory.api.domain.like.dto.request.LikeBoardRequest;
import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.like.dto.command.LikeBoardCommand;
import com.tagstory.core.domain.like.service.Like;
import com.tagstory.core.domain.user.service.User;
import com.tagstory.core.utils.lock.LockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LikeLockAspect {
    private final LockManager lockManager;

    /**
     * LikeController의 like()가 실행될 때 락을 건다.
     * @param likeBoardRequest
     * @param userId
     */
    @Before(value = "execution(* com.tagstory.api.domain.like.LikeController.like(..)) && args(userId, likeBoardRequest)"
            , argNames = "userId,likeBoardRequest")
    public void beforeLikeController(Long userId, LikeBoardRequest likeBoardRequest) {
        log.info("Before Controller Executed");
        String name = getLockName(userId, likeBoardRequest.getBoardId());
        lockManager.lock(name);
    }

    /**
     * LikeFacade가 실행되기 전에 락의 획득 여부를 반환한다.
     * @param command
     * @return
     */
    @Before(value = "execution(* com.tagstory.core.domain.like.service.LikeFacade.like(..)) && args(command)")
    public Boolean beforeLikeFacade(LikeBoardCommand command) {
        log.info("Before Facade Executed");
        String name = getLockName(command.getUserId(), command.getBoardId());
        return lockManager.isLocked(name);
    }

    /**
     * LikeService의 like()가 종료되면 락을 해제한다.
     * @param board
     * @param user
     */
    @After(value = "execution(* com.tagstory.core.domain.like.service.LikeService.like(..)) && args(board, user)"
            , argNames = "board,user")
    public void afterLikeService(Board board, User user) {
        log.info("After Service Executed");
        String name = Like.getLockName(user.getUserId(), board.getBoardId());
        lockManager.unlock(name);
    }

    /**
     * 락의 이름을 반환한다.
     * @param userId
     * @param boardId
     * @return
     */
    private String getLockName(Long userId, String boardId) {
        return Like.getLockName(userId, boardId);
    }
}

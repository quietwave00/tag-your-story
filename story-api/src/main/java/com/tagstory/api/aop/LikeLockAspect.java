package com.tagstory.api.aop;

import com.tagstory.api.domain.like.dto.request.LikeBoardRequest;
import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.like.dto.command.LikeBoardCommand;
import com.tagstory.core.domain.like.service.Like;
import com.tagstory.core.domain.user.service.User;
import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
import com.tagstory.core.utils.api.ApiUtils;
import com.tagstory.core.utils.lock.LockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LikeLockAspect {
    private final LockManager lockManager;

    /**
     * LikeController의 like()가 실행될 때 락 획득 여부에 따라 동작을 수행한다.
     */
    @Before(value = "execution(* com.tagstory.api.domain.like.LikeController.like(..)) && args(likeBoardRequest, userId)"
            , argNames = "userId,likeBoardRequest")
    public void beforeLikeController(Long userId, LikeBoardRequest likeBoardRequest) {
        String name = getLockName(userId, likeBoardRequest.getBoardId());
        if(lockManager.isLocked(name)) {
            throw new CustomException(ExceptionCode.LOCKED_RESOURCE);
        }
        lockManager.lock(name);
    }

    /**
     * LikeService의 like()가 종료되면 락을 해제한다.
     */
    @After(value = "execution(* com.tagstory.core.domain.like.service.LikeService.like(..)) && args(board, user)"
            , argNames = "board,user")
    public void afterLikeService(Board board, User user) {
        String name = getLockName(user.getUserId(), board.getBoardId());
        lockManager.unlock(name);
    }

    /**
     * 락의 이름을 반환한다.
     */
    private String getLockName(Long userId, String boardId) {
        return Like.getLockName(userId, boardId);
    }
}

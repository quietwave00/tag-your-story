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
    private final RedissonClient redissonClient;

//    private final LockManager lockManager;
    public Boolean like(LikeBoardCommand command) {
        /* api 중복 요청 방지를 위한 분산 락 적용 */
//        lockManager.lock(Like.getLockNameOfKey(command.getUserId(), command.getBoardId()));
        final RLock lock = redissonClient.getLock(Like.getLockNameOfKey(command.getUserId(), command.getBoardId()));
        if(lock.isLocked()) {
            log.info("걸림 ㅠㅠ!!!!!");
            return false;
        }

        try {
            boolean available = lock.tryLock(2, 3, TimeUnit.SECONDS);
            log.info("available: " + available);
            if(!available) {

            }
            log.info("락 진입");

            boardService.increaseLikeCount(command.getBoardId());
            Board board = boardService.getBoardByBoardId(command.getBoardId());
            User user = userService.getCacheByUserId(command.getUserId());

            LikeEntity like = LikeEntity.createLike(user, board);
            likeRepository.save(like);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if(lock.isLocked()) {
                lock.unlock();
                log.info("락 해제");
            }
        }
        return true;
    }

    @Transactional
    public void unLike(UnLikeCommand command) {
        boardService.decreaseLikeCount(command.getBoardId());

        /* 데이터 정합성을 위하여 대상 Like 객체에 락을 건다. */
        likeService.findByBoardIdAndUserId(command.getBoardId(), command.getUserId());
//        lockManager.lock(Like.getLockNameOfKey(command.getUserId(), command.getBoardId()));

        likeService.unLike(command.getBoardId(), command.getUserId());
    }

    public boolean isLiked(String boardId, Long userId) {
        return likeService.isLiked(boardId, userId);
    }
}

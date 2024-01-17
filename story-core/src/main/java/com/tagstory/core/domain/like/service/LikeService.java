package com.tagstory.core.domain.like.service;

import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.event.publisher.CommonEventPublisher;
import com.tagstory.core.domain.like.LikeEntity;
import com.tagstory.core.domain.like.repository.LikeRepository;
import com.tagstory.core.domain.user.service.User;
import com.tagstory.core.utils.lock.LockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LikeService {
    private final LikeRepository likeRepository;
    private final CommonEventPublisher eventPublisher;

    private final LockManager lockManager;

    @Transactional
    public void like(Board board, User user) {
        LikeEntity like = LikeEntity.createLike(user, board);
        LikeEntity savedLike = likeRepository.save(like);

        /* api 중복 요청 방지를 위한 분산락 적용 */
        lockManager.lock(Like.getLockNameOfKey(savedLike.getLikeId()));

        /* 좋아요를 누른 유저가 글쓴이가 아닌 경우에만 알림 이벤트를 발행한다. */
        if(!isWriter(user, board)) {
            eventPublisher.onEventFromLike(user, board);
        }
    }


    public void unLike(String boardId, Long userId) {
        likeRepository.deleteByBoard_BoardId_AndUser_UserId(boardId, userId);
    }

    public boolean isLiked(String boardId, Long userId) {
        Like findedLike = findByBoardIdAndUserId(boardId, userId);
        return Objects.nonNull(findedLike);
    }

    private boolean isWriter(User user, Board board) {
        return Objects.equals(user.getUserId(), board.getUser().getUserId());
    }

    public Like findByBoardIdAndUserId(String boardId, Long userId) {
        Optional<LikeEntity> likeEntityOptional =
                likeRepository.findByBoard_BoardId_AndUser_UserId(boardId, userId);

        return likeEntityOptional.map(LikeEntity::toLike).orElse(null);
    }
}

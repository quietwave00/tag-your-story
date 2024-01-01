package com.tagstory.core.domain.like.service;

import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.event.publisher.CommonEventPublisher;
import com.tagstory.core.domain.like.LikeEntity;
import com.tagstory.core.domain.like.repository.LikeRepository;
import com.tagstory.core.domain.user.service.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {
    private final LikeRepository likeRepository;
    private final CommonEventPublisher eventPublisher;

    @Transactional
    public void like(Board board, User user) {
        LikeEntity like = LikeEntity.createLike(user, board);
        likeRepository.save(like);

        /* 좋아요를 누른 유저가 글쓴이가 아닌 경우에만 알림 이벤트를 발행한다. */
        if(!isWriter(user, board)) {
            eventPublisher.onEventFromLike(user, board);
        }
    }

    @Transactional
    public void unLike(Board board, User user) {
        likeRepository.deleteByBoardAndUser(board.toEntity(), user.toEntity());
    }

    public boolean isLiked(String boardId, Long userId) {
        LikeEntity likeEntity = likeRepository.findByBoard_BoardId_AndUser_UserId(boardId, userId);
        return Objects.nonNull(likeEntity);
    }

    private boolean isWriter(User user, Board board) {
        return Objects.equals(user.getUserId(), board.getUser().getUserId());
    }
}

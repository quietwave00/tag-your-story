package com.tagstory.core.domain.like.service;

import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.like.LikeEntity;
import com.tagstory.core.domain.like.repository.LikeRepository;
import com.tagstory.core.domain.notification.NotificationType;
import com.tagstory.core.domain.notification.service.Notification;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {
    private final LikeRepository likeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CompletableFuture<Void> like(Board board, User user) {
        LikeEntity like = LikeEntity.createLike(user, board);
        likeRepository.save(like);

        return isWriter(board, user) ?
                CompletableFuture.completedFuture(null)
                : CompletableFuture.runAsync(() -> onEvent(user, board));
    }

    @Transactional
    public void unLike(Board board, User user) {
        likeRepository.deleteByBoardAndUser(board.toEntity(), user.toEntity());
    }

    public boolean isLiked(String boardId, Long userId) {
        LikeEntity likeEntity = likeRepository.findByBoard_BoardId_AndUser_UserId(boardId, userId);
        return Objects.nonNull(likeEntity);
    }

    private void onEvent(User user, Board board) {
        eventPublisher.publishEvent(Notification.onEvent(user,
                board.getUser(),
                NotificationType.LIKE,
                board.getBoardId()
        ));
    }

    private boolean isWriter(Board board, User user) {
        return Objects.equals(board.getUser().getUserId(), user.getUserId());
    }
}

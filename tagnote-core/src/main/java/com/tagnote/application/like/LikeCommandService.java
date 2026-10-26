package com.tagnote.application.like;

import com.tagnote.application.user.ActiveUserWriteGuard;
import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.event.publisher.CommonEventPublisher;
import com.tagnote.core.domain.like.LikeEntity;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.infrastructure.persistence.board.BoardJpaRepository;
import com.tagnote.infrastructure.persistence.like.LikeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LikeCommandService {
    private final ActiveUserWriteGuard activeUserWriteGuard;
    private final BoardJpaRepository boards;
    private final LikeJpaRepository likes;
    private final CommonEventPublisher events;

    @Transactional
    public Boolean like(String boardId, Long userId) {
        UserEntity user = activeUserWriteGuard.lock(userId);
        BoardEntity board = boards.findLikeTarget(boardId, BoardStatus.POST)
                .orElseThrow(() -> new CustomException(ExceptionCode.BOARD_NOT_FOUND));
        try {
            // IDENTITY generation inserts on save; the DB unique key is the concurrency guard.
            likes.save(LikeEntity.createLike(user, board));
        } catch (DataIntegrityViolationException failure) {
            if (isLikeUniqueConflict(failure)) {
                throw new CustomException(ExceptionCode.LIKE_ALREADY_EXISTS);
            }
            throw failure;
        }
        if (boards.updateLikeCount(boardId, 1) != 1) {
            throw new CustomException(ExceptionCode.BOARD_NOT_FOUND);
        }
        if (!Objects.equals(userId, board.getUserEntity().getUserId())) {
            events.onEventFromLike(user.toUser(), Board.builder()
                    .boardId(boardId).user(board.getUserEntity().toUser()).build());
        }
        return true;
    }

    @Transactional
    public void unLike(String boardId, Long userId) {
        activeUserWriteGuard.lock(userId);
        if (likes.deleteByBoardIdAndUserId(boardId, userId) == 1
                && boards.updateLikeCount(boardId, -1) != 1) {
            throw new CustomException(ExceptionCode.BOARD_NOT_FOUND);
        }
    }

    private boolean isLikeUniqueConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                return name != null && name.toLowerCase(Locale.ROOT).contains("uk_board_like_user_board");
            }
        }
        return false;
    }
}

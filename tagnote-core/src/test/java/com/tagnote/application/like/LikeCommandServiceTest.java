package com.tagnote.application.like;

import com.tagnote.application.user.ActiveUserWriteGuard;
import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.event.publisher.CommonEventPublisher;
import com.tagnote.core.domain.like.LikeEntity;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.infrastructure.persistence.board.BoardJpaRepository;
import com.tagnote.infrastructure.persistence.like.LikeJpaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeCommandServiceTest {
    @Mock ActiveUserWriteGuard guard;
    @Mock BoardJpaRepository boards;
    @Mock LikeJpaRepository likes;
    @Mock CommonEventPublisher events;
    @InjectMocks LikeCommandService service;

    private UserEntity user(Long id) {
        return UserEntity.builder().userId(id).userKey("u" + id).email("u" + id + "@test.com")
                .nickname("user" + id).role(Role.ROLE_USER).userStatus(UserStatus.ACTIVE).build();
    }

    @Test
    void 신규_좋아요만_counter와_타인_알림을_갱신한다() {
        when(guard.lock(7L)).thenReturn(user(7L));
        when(boards.findLikeTarget("board-1", BoardStatus.POST)).thenReturn(Optional.of(
                BoardEntity.builder().boardId("board-1").userEntity(user(8L)).build()));
        when(boards.updateLikeCount("board-1", 1)).thenReturn(1);

        assertThat(service.like("board-1", 7L)).isTrue();
        verify(likes).save(any(LikeEntity.class));
        verify(events).onEventFromLike(any(), any());
    }

    @Test
    void 본인_글은_알림이_없다() {
        when(guard.lock(7L)).thenReturn(user(7L));
        when(boards.findLikeTarget("board-1", BoardStatus.POST)).thenReturn(Optional.of(
                BoardEntity.builder().boardId("board-1").userEntity(user(7L)).build()));
        when(boards.updateLikeCount("board-1", 1)).thenReturn(1);

        service.like("board-1", 7L);
        verifyNoInteractions(events);
    }

    @Test
    void 지정된_unique_충돌만_409로_변환한다() {
        when(guard.lock(7L)).thenReturn(user(7L));
        when(boards.findLikeTarget("board-1", BoardStatus.POST)).thenReturn(Optional.of(
                BoardEntity.builder().boardId("board-1").userEntity(user(8L)).build()));
        ConstraintViolationException violation = mock(ConstraintViolationException.class);
        when(violation.getConstraintName()).thenReturn("UK_BOARD_LIKE_USER_BOARD");
        when(likes.save(any(LikeEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate", violation));

        assertThatThrownBy(() -> service.like("board-1", 7L))
                .isInstanceOfSatisfying(CustomException.class,
                        error -> assertThat(error.getExceptionCode()).isEqualTo(ExceptionCode.LIKE_ALREADY_EXISTS));
        verify(boards, never()).updateLikeCount(any(), anyInt());
        verifyNoInteractions(events);
    }

    @Test
    void 다른_제약_오류는_좋아요_중복으로_변환하지_않는다() {
        when(guard.lock(7L)).thenReturn(user(7L));
        when(boards.findLikeTarget("board-1", BoardStatus.POST)).thenReturn(Optional.of(
                BoardEntity.builder().boardId("board-1").userEntity(user(8L)).build()));
        ConstraintViolationException violation = mock(ConstraintViolationException.class);
        when(violation.getConstraintName()).thenReturn("FK_BOARD_LIKE_BOARD");
        DataIntegrityViolationException failure = new DataIntegrityViolationException("fk", violation);
        when(likes.save(any(LikeEntity.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.like("board-1", 7L)).isSameAs(failure);
        verify(boards, never()).updateLikeCount(any(), anyInt());
    }

    @Test
    void 없는_관계의_재취소는_counter를_변경하지_않는다() {
        when(likes.deleteByBoardIdAndUserId("board-1", 7L)).thenReturn(0);
        service.unLike("board-1", 7L);
        verify(guard).lock(7L);
        verify(boards, never()).updateLikeCount(any(), anyInt());
    }

    @Test
    void 제거된_관계만_counter를_감소시킨다() {
        when(likes.deleteByBoardIdAndUserId("board-1", 7L)).thenReturn(1);
        when(boards.updateLikeCount("board-1", -1)).thenReturn(1);
        service.unLike("board-1", 7L);
        verify(boards).updateLikeCount("board-1", -1);
    }

    @Test
    void counter_update가_실패하면_알림을_발행하지_않고_예외로_트랜잭션을_실패시킨다() {
        when(guard.lock(7L)).thenReturn(user(7L));
        when(boards.findLikeTarget("board-1", BoardStatus.POST)).thenReturn(Optional.of(
                BoardEntity.builder().boardId("board-1").userEntity(user(8L)).build()));
        when(boards.updateLikeCount("board-1", 1)).thenReturn(0);

        assertThatThrownBy(() -> service.like("board-1", 7L))
                .isInstanceOfSatisfying(CustomException.class,
                        error -> assertThat(error.getExceptionCode()).isEqualTo(ExceptionCode.BOARD_NOT_FOUND));
        verifyNoInteractions(events);
    }
}

package com.tagnote.domain.comment;

import com.tagnote.application.user.ActiveUserWriteGuard;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.comment.CommentEntity;
import com.tagnote.core.domain.comment.CommentStatus;
import com.tagnote.core.domain.comment.repository.CommentRepository;
import com.tagnote.core.domain.comment.service.Comment;
import com.tagnote.core.domain.comment.service.CommentService;
import com.tagnote.core.domain.comment.service.dto.command.CreateCommentCommand;
import com.tagnote.core.domain.comment.service.dto.command.CreateReplyCommand;
import com.tagnote.core.domain.comment.service.dto.command.UpdateCommentCommand;
import com.tagnote.core.domain.comment.service.dto.response.CommentWithReplies;
import com.tagnote.core.domain.event.publisher.CommonEventPublisher;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.domain.board.fixture.BoardFixture;
import com.tagnote.domain.comment.fixture.CommentFixture;
import com.tagnote.domain.user.fixture.UserFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ActiveUserWriteGuard activeUserWriteGuard;

    @Mock
    private CommonEventPublisher eventPublisher;

    @InjectMocks
    private CommentService commentService;

    @Test
    void 댓글을_작성한다() {
        // given
        CreateCommentCommand command = CreateCommentCommand.builder()
                .userId(1L)
                .boardId("1")
                .content("content")
                .build();

        CommentEntity mockSavedComment = CommentFixture.createCommentEntity(1L, command.getContent());
        mockSavedComment.addUser(UserEntity.builder().userId(command.getUserId()).build());
        mockSavedComment.addBoard(BoardFixture.createBoardEntityWithUserEntity());

        // when
        when(commentRepository.save(any())).thenReturn(mockSavedComment);
        Comment result = commentService.create(BoardFixture.createBoardWithUser("1", 1L),
                UserFixture.createUser(command.getUserId()),
                command);

        // then
        assertThat(command.getContent()).isEqualTo(result.getContent());
        assertThat(command.getBoardId()).isEqualTo(result.getBoard().getBoardId());
        verify(activeUserWriteGuard).lock(1L);
        verify(eventPublisher, never()).onEventFromComment(any(), any());
    }

    @Test
    void 다른_작성자의_댓글은_알림을_발행한다() {
        Board board = BoardFixture.createBoardWithUser("1", 2L);
        User user = UserFixture.createUser(1L);
        when(commentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Comment result = commentService.create(board, user, CreateCommentCommand.builder()
                .userId(1L).boardId("1").content("content").build());

        assertThat(result.getStatus()).isEqualTo(CommentStatus.POST);
        verify(activeUserWriteGuard).lock(1L);
        verify(eventPublisher).onEventFromComment(user, board);
    }

    @Test
    void 답글은_부모에_연결하고_알림을_발행하지_않는다() {
        CommentEntity parent = comment(3L, "parent");
        when(commentRepository.findById(3L)).thenReturn(Optional.of(parent));
        when(commentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Comment result = commentService.createReply(BoardFixture.createBoardWithUser("1", 2L),
                UserFixture.createUser(1L), CreateReplyCommand.builder()
                        .userId(1L).boardId("1").parentId(3L).content("reply").build());

        assertThat(result.getParent().getCommentId()).isEqualTo(3L);
        assertThat(parent.getChildren()).hasSize(1);
        verify(activeUserWriteGuard).lock(1L);
        verify(eventPublisher, never()).onEventFromComment(any(), any());
    }

    @Test
    void 없는_부모는_COMMENT_NOT_FOUND이다() {
        when(commentRepository.findById(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.createReply(BoardFixture.createBoardWithUser("1", 2L),
                UserFixture.createUser(1L), CreateReplyCommand.builder().userId(1L).parentId(3L).build()))
                .isInstanceOf(CustomException.class)
                .extracting("exceptionCode").isEqualTo(ExceptionCode.COMMENT_NOT_FOUND);
    }

    @Test
    void 수정과_삭제는_기존_엔티티를_변경한다() {
        CommentEntity entity = comment(3L, "old");
        when(commentRepository.findById(3L)).thenReturn(Optional.of(entity));

        assertThat(commentService.update(UpdateCommentCommand.builder().commentId(3L).content("new").build()).getContent())
                .isEqualTo("new");
        commentService.delete(3L);
        assertThat(entity.getStatus()).isEqualTo(CommentStatus.REMOVAL);
    }

    @Test
    void 목록은_POST_최상위와_POST_자식만_반환한다() {
        CommentEntity parent = comment(3L, "parent");
        CommentEntity posted = comment(4L, "posted");
        CommentEntity removed = comment(5L, "removed");
        removed.delete();
        posted.addParent(parent);
        removed.addParent(parent);
        when(commentRepository.findByStatusAndParentIsNullAndBoardEntity_BoardIdOrderByCommentIdDesc(
                eq(CommentStatus.POST), eq("1"), eq(PageRequest.of(2, 20))))
                .thenReturn(Optional.of(new PageImpl<>(List.of(parent))));

        List<CommentWithReplies> result = commentService.getCommentList("1", 2);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChildren()).extracting(com.tagnote.core.domain.comment.service.Comment::getCommentId)
                .containsExactly(4L);
    }

    @Test
    void 답글은_5개씩_조회하고_카운트는_POST_전체다() {
        CommentEntity parent = comment(3L, "parent");
        CommentEntity reply = comment(4L, "reply");
        when(commentRepository.findById(3L)).thenReturn(Optional.of(parent));
        when(commentRepository.findByParentAndStatus(any(), eq(CommentStatus.POST), eq(PageRequest.of(1, 5))))
                .thenReturn(new PageImpl<>(List.of(reply)));
        when(commentRepository.countByBoardEntity_BoardIdAndStatus("1", CommentStatus.POST)).thenReturn(3);

        assertThat(commentService.getReplyList(3L, 1)).extracting(com.tagnote.core.domain.comment.service.Comment::getCommentId)
                .containsExactly(4L);
        assertThat(commentService.getCommentCountByBoardId("1")).isEqualTo(3);
    }

    @Test
    void 작성자_ID_목록은_삭제_상태도_포함한다() {
        CommentEntity removed = comment(5L, "removed");
        removed.delete();
        when(commentRepository.findByBoardEntity_BoardIdAndUserEntity_UserId("1", 1L))
                .thenReturn(List.of(removed));
        assertThat(commentService.getUserCommentId("1", 1L)).containsExactly(5L);
    }

    @Test
    void 탈퇴_작성자의_댓글도_공개되고_표시명이_대체된다() {
        CommentEntity old = comment(3L, "old");
        old.addUser(UserEntity.builder().userId(1L)
                .userStatus(com.tagnote.core.domain.user.UserStatus.WITHDRAWN).build());
        when(commentRepository.findByStatusAndParentIsNullAndBoardEntity_BoardIdOrderByCommentIdDesc(
                eq(CommentStatus.POST), eq("1"), eq(PageRequest.of(0, 20))))
                .thenReturn(Optional.of(new PageImpl<>(List.of(old))));

        assertThat(commentService.getCommentList("1", 0).get(0).getComment().getUser().getDisplayNickname())
                .isEqualTo("탈퇴한 사용자");
    }

    private CommentEntity comment(Long id, String content) {
        CommentEntity entity = CommentEntity.builder().commentId(id).content(content)
                .status(CommentStatus.POST).children(new ArrayList<>()).build();
        entity.addUser(UserEntity.builder().userId(1L).nickname("writer").build());
        entity.addBoard(BoardFixture.createBoardEntityWithUserEntity());
        return entity;
    }
}

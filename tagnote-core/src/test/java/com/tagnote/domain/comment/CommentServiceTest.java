package com.tagnote.domain.comment;

import com.tagnote.core.domain.comment.CommentEntity;
import com.tagnote.core.domain.comment.repository.CommentRepository;
import com.tagnote.core.domain.comment.service.CommentService;
import com.tagnote.core.domain.comment.service.Comment;
import com.tagnote.core.domain.comment.service.dto.command.CreateCommentCommand;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.domain.board.fixture.BoardFixture;
import com.tagnote.domain.comment.fixture.CommentFixture;
import com.tagnote.domain.user.fixture.UserFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

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
    }
}

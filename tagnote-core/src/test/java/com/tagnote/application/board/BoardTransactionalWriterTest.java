package com.tagnote.application.board;

import com.tagnote.application.board.model.BoardSubject;
import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import com.tagnote.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.service.UserTagService;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.infrastructure.persistence.board.BoardJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardTransactionalWriterTest {

    @Mock
    private BoardJpaRepository boardRepository;
    @Mock
    private BoardUserTagRepository boardUserTagRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TrackJpaRepository trackRepository;
    @Mock
    private AlbumJpaRepository albumRepository;
    @Mock
    private UserTagService userTagService;

    @InjectMocks
    private BoardTransactionalWriter writer;

    @Test
    void Track_Board와_writer_소유_UserTag를_한_write_경계에_저장한다() {
        UserEntity owner = user(1L);
        TrackEntity track = org.mockito.Mockito.mock(TrackEntity.class);
        UserTagEntity userTag = UserTagEntity.builder().userTagId(11L).owner(owner).name("Jazz").build();
        CreateBoardCommand command = createCommand("spotify-track", null);
        BoardEntity saved = BoardEntity.builder()
                .boardId("board-1")
                .content("content")
                .status(BoardStatus.POST)
                .userEntity(owner)
                .track(track)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(trackRepository.findById(10L)).thenReturn(Optional.of(track));
        when(boardRepository.saveAndFlush(any(BoardEntity.class))).thenReturn(saved);
        when(userTagService.makeUserTagList(owner, List.of("Jazz"))).thenReturn(List.of(userTag));

        assertThat(writer.create(command, BoardSubject.track(10L, "spotify-track")))
                .isEqualTo("board-1");

        ArgumentCaptor<BoardEntity> boardCaptor = ArgumentCaptor.forClass(BoardEntity.class);
        verify(boardRepository).saveAndFlush(boardCaptor.capture());
        assertThat(boardCaptor.getValue().getTrack()).isSameAs(track);
        assertThat(boardCaptor.getValue().getAlbum()).isNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BoardUserTagEntity>> joinCaptor = ArgumentCaptor.forClass(List.class);
        verify(boardUserTagRepository).saveAll(joinCaptor.capture());
        assertThat(joinCaptor.getValue()).singleElement().satisfies(join -> {
            assertThat(join.getBoard()).isSameAs(saved);
            assertThat(join.getUserTag()).isSameAs(userTag);
        });
        verify(albumRepository, never()).findById(any());
    }

    @Test
    void Album_Board는_album_FK만_설정한다() {
        UserEntity owner = user(1L);
        AlbumEntity album = org.mockito.Mockito.mock(AlbumEntity.class);
        CreateBoardCommand command = createCommand(null, "spotify-album");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(albumRepository.findById(20L)).thenReturn(Optional.of(album));
        when(boardRepository.saveAndFlush(any(BoardEntity.class))).thenAnswer(invocation -> {
            BoardEntity board = invocation.getArgument(0);
            return BoardEntity.builder()
                    .boardId("board-2")
                    .content(board.getContent())
                    .status(board.getStatus())
                    .userEntity(board.getUserEntity())
                    .album(board.getAlbum())
                    .build();
        });
        when(userTagService.makeUserTagList(owner, List.of("Jazz"))).thenReturn(List.of());

        assertThat(writer.create(command, BoardSubject.album(20L, "spotify-album")))
                .isEqualTo("board-2");

        ArgumentCaptor<BoardEntity> captor = ArgumentCaptor.forClass(BoardEntity.class);
        verify(boardRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getAlbum()).isSameAs(album);
        assertThat(captor.getValue().getTrack()).isNull();
        verify(trackRepository, never()).findById(any());
    }

    @Test
    void 수정은_writer만_허용하고_non_empty_UserTag를_교체한다() {
        UserEntity owner = user(1L);
        BoardEntity board = BoardEntity.builder()
                .boardId("board-1")
                .content("before")
                .status(BoardStatus.POST)
                .userEntity(owner)
                .build();
        UserTagEntity replacement = UserTagEntity.builder()
                .userTagId(12L).owner(owner).name("Ambient").build();
        UpdateBoardCommand command = UpdateBoardCommand.builder()
                .boardId("board-1").content("after").userTagList(List.of("Ambient")).build();
        when(boardRepository.findByBoardIdAndStatus("board-1", BoardStatus.POST))
                .thenReturn(Optional.of(board));
        when(userTagService.makeUserTagList(owner, List.of("Ambient")))
                .thenReturn(List.of(replacement));

        assertThat(writer.update(command, 1L)).isEqualTo("board-1");

        assertThat(board.getContent()).isEqualTo("after");
        verify(boardUserTagRepository).deleteByBoard_BoardId("board-1");
        verify(boardUserTagRepository).saveAll(any());
    }

    @Test
    void 작성자가_아니면_수정하거나_태그를_변경하지_않는다() {
        BoardEntity board = BoardEntity.builder()
                .boardId("board-1")
                .content("before")
                .status(BoardStatus.POST)
                .userEntity(user(1L))
                .build();
        UpdateBoardCommand command = UpdateBoardCommand.builder()
                .boardId("board-1").content("after").userTagList(List.of("Ambient")).build();
        when(boardRepository.findByBoardIdAndStatus("board-1", BoardStatus.POST))
                .thenReturn(Optional.of(board));

        assertThatThrownBy(() -> writer.update(command, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("exceptionCode")
                .isEqualTo(ExceptionCode.BOARD_ACCESS_DENIED);

        assertThat(board.getContent()).isEqualTo("before");
        verify(userTagService, never()).makeUserTagList(any(), any());
        verify(boardUserTagRepository, never()).deleteByBoard_BoardId(any());
    }

    @Test
    void 빈_update_UserTag_목록은_기존_연결을_유지한다() {
        BoardEntity board = BoardEntity.builder()
                .boardId("board-1")
                .content("before")
                .status(BoardStatus.POST)
                .userEntity(user(1L))
                .build();
        UpdateBoardCommand command = UpdateBoardCommand.builder()
                .boardId("board-1").content("after").userTagList(List.of()).build();
        when(boardRepository.findByBoardIdAndStatus("board-1", BoardStatus.POST))
                .thenReturn(Optional.of(board));

        writer.update(command, 1L);

        assertThat(board.getContent()).isEqualTo("after");
        verify(userTagService, never()).makeUserTagList(any(), any());
        verify(boardUserTagRepository, never()).deleteByBoard_BoardId(any());
    }

    @Test
    void 삭제는_writer만_soft_delete할_수_있다() {
        BoardEntity board = BoardEntity.builder()
                .boardId("board-1")
                .status(BoardStatus.POST)
                .userEntity(user(1L))
                .build();
        when(boardRepository.findByBoardIdAndStatus("board-1", BoardStatus.POST))
                .thenReturn(Optional.of(board));

        writer.delete("board-1", 1L);

        assertThat(board.getStatus()).isEqualTo(BoardStatus.REMOVAL);
    }

    @Test
    void 작성자가_아니면_삭제할_수_없다() {
        BoardEntity board = BoardEntity.builder()
                .boardId("board-1")
                .status(BoardStatus.POST)
                .userEntity(user(1L))
                .build();
        when(boardRepository.findByBoardIdAndStatus("board-1", BoardStatus.POST))
                .thenReturn(Optional.of(board));

        assertThatThrownBy(() -> writer.delete("board-1", 2L))
                .isInstanceOf(CustomException.class)
                .extracting("exceptionCode")
                .isEqualTo(ExceptionCode.BOARD_ACCESS_DENIED);

        assertThat(board.getStatus()).isEqualTo(BoardStatus.POST);
    }

    private UserEntity user(long userId) {
        return UserEntity.builder().userId(userId).nickname("writer").build();
    }

    private CreateBoardCommand createCommand(String trackId, String albumId) {
        return CreateBoardCommand.builder()
                .userId(1L)
                .content("content")
                .trackId(trackId)
                .albumId(albumId)
                .userTagList(List.of("Jazz"))
                .build();
    }
}

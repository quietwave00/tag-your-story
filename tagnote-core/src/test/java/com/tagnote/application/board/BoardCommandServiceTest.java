package com.tagnote.application.board;

import com.tagnote.application.board.model.BoardSubject;
import com.tagnote.application.catalog.detail.model.AlbumDetail;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.importer.model.ImportedAlbum;
import com.tagnote.application.catalog.importer.model.ImportedTrack;
import com.tagnote.application.catalog.selection.AlbumSelectionService;
import com.tagnote.application.catalog.selection.TrackSelectionService;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardCommandServiceTest {

    @Mock
    private TrackSelectionService trackSelectionService;
    @Mock
    private AlbumSelectionService albumSelectionService;
    @Mock
    private BoardTransactionalWriter writer;
    @Mock
    private BoardQueryService queryService;

    @InjectMocks
    private BoardCommandService service;

    @Test
    void trackId는_Track_selection과_TRACK_subject_write로_연결한다() {
        CreateBoardCommand command = command("spotify-track", null);
        ImportedTrack track = ImportedTrack.builder()
                .catalogTrackId(10L).spotifyTrackId("spotify-track").artists(List.of()).build();
        when(trackSelectionService.select("spotify-track")).thenReturn(new TrackDetail(track, List.of()));
        when(writer.create(any(), any())).thenReturn("board-1");

        service.create(command);

        verify(writer).create(command, BoardSubject.track(10L, "spotify-track"));
        verify(albumSelectionService, never()).select(any());
    }

    @Test
    void albumId는_Album_selection과_ALBUM_subject_write로_연결한다() {
        CreateBoardCommand command = command(null, "spotify-album");
        ImportedAlbum album = ImportedAlbum.of(20L, "spotify-album", "album", 2026, List.of());
        when(albumSelectionService.select("spotify-album")).thenReturn(new AlbumDetail(album, List.of()));
        when(writer.create(any(), any())).thenReturn("board-2");

        service.create(command);

        verify(writer).create(command, BoardSubject.album(20L, "spotify-album"));
        verify(trackSelectionService, never()).select(any());
    }

    @Test
    void subject가_없거나_둘다_있으면_selection과_write를_호출하지_않는다() {
        assertInvalid(command(null, null));
        assertInvalid(command("track", "album"));

        verify(trackSelectionService, never()).select(any());
        verify(albumSelectionService, never()).select(any());
        verify(writer, never()).create(any(), any());
    }

    @Test
    void selection이_실패하면_Board_write를_호출하지_않는다() {
        CreateBoardCommand command = command("spotify-track", null);
        when(trackSelectionService.select("spotify-track"))
                .thenThrow(new CustomException(ExceptionCode.TRACK_NOT_FOUND));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(CustomException.class)
                .extracting("exceptionCode")
                .isEqualTo(ExceptionCode.TRACK_NOT_FOUND);

        verify(writer, never()).create(any(), any());
    }

    private void assertInvalid(CreateBoardCommand command) {
        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(CustomException.class)
                .extracting("exceptionCode")
                .isEqualTo(ExceptionCode.INVALID_BOARD_SUBJECT);
    }

    private CreateBoardCommand command(String trackId, String albumId) {
        return CreateBoardCommand.builder()
                .userId(1L)
                .content("content")
                .trackId(trackId)
                .albumId(albumId)
                .userTagList(List.of("tag"))
                .build();
    }
}

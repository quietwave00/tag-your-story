package com.tagnote.domain.board;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BoardEntityTest {

    private final UserEntity writer = UserEntity.builder().userId(1L).build();

    @Test
    void Track_Board는_Track만_대상으로_가진다() {
        TrackEntity track = mock(TrackEntity.class);

        BoardEntity board = BoardEntity.createForTrack("content", writer, track);

        assertThat(board.getTrack()).isSameAs(track);
        assertThat(board.getAlbum()).isNull();
        assertThat(board.getStatus()).isEqualTo(BoardStatus.POST);
    }

    @Test
    void Album_Board는_Album만_대상으로_가진다() {
        AlbumEntity album = mock(AlbumEntity.class);

        BoardEntity board = BoardEntity.createForAlbum("content", writer, album);

        assertThat(board.getAlbum()).isSameAs(album);
        assertThat(board.getTrack()).isNull();
    }

    @Test
    void target이_null이면_생성을_거부한다() {
        assertThatThrownBy(() -> BoardEntity.createForTrack("content", writer, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BoardEntity.createForAlbum("content", writer, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 삭제는_REMOVAL_soft_delete다() {
        BoardEntity board = BoardEntity.createForTrack("content", writer, mock(TrackEntity.class));

        board.delete();

        assertThat(board.getStatus()).isEqualTo(BoardStatus.REMOVAL);
    }
}

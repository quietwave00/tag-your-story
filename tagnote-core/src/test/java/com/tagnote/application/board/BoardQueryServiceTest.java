package com.tagnote.application.board;

import com.tagnote.application.board.model.BoardPage;
import com.tagnote.application.board.model.BoardView;
import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardOrderType;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.resolution.ResolvedStatus;
import com.tagnote.domain.resolution.SubjectTagResolvedEntity;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.infrastructure.persistence.board.BoardJpaRepository;
import com.tagnote.infrastructure.persistence.board.BoardUserTagNameRow;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardQueryServiceTest {

    @Mock
    private BoardJpaRepository boardRepository;
    @Mock
    private BoardUserTagRepository boardUserTagRepository;
    @Mock
    private SubjectTagResolvedJpaRepository resolvedRepository;

    @Test
    void 목록의_UserTag와_Track_SystemTag를_각각_bulk로_조합한다() {
        BoardEntity first = trackBoard("board-1", 10L, "spotify-track");
        BoardEntity second = trackBoard("board-2", 10L, "spotify-track");
        when(boardRepository.findByStatusAndTrack_SpotifyIdOrderByCreatedAtDesc(
                eq(BoardStatus.POST), eq("spotify-track"), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(first, second)));
        when(boardUserTagRepository.findNamesByBoardIds(List.of("board-1", "board-2")))
                .thenReturn(List.of(
                        new BoardUserTagNameRow("board-1", "Jazz"),
                        new BoardUserTagNameRow("board-2", "Favorite")
                ));
        SubjectTagResolvedEntity resolved = resolved(SubjectType.TRACK, 10L, 1L, "Ambient", 0.9);
        when(resolvedRepository.findVisibleBySubjectsWithTag(
                SubjectType.TRACK, Set.of(10L), ResolvedStatus.HIDDEN
        )).thenReturn(List.of(resolved));

        BoardQueryService service = service();
        BoardPage result = service.getTrackBoards("spotify-track", BoardOrderType.CREATED_AT, 0);

        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.boards()).extracting(BoardView::boardId)
                .containsExactly("board-1", "board-2");
        assertThat(result.boards().get(0).userTagNames()).containsExactly("Jazz");
        assertThat(result.boards().get(1).userTagNames()).containsExactly("Favorite");
        assertThat(result.boards()).allSatisfy(board -> {
            assertThat(board.subject().type()).isEqualTo(SubjectType.TRACK);
            assertThat(board.subject().spotifyId()).isEqualTo("spotify-track");
            assertThat(board.systemTags()).singleElement().satisfies(tag -> {
                assertThat(tag.tagId()).isEqualTo(1L);
                assertThat(tag.name()).isEqualTo("Ambient");
                assertThat(tag.score()).isEqualTo(0.9);
            });
        });
        verify(resolvedRepository, never()).findVisibleBySubjectsWithTag(
                eq(SubjectType.ALBUM), any(), eq(ResolvedStatus.HIDDEN)
        );
    }

    @Test
    void UserTag_조회의_mixed_subject는_type별로_SystemTag를_한번씩_조회한다() {
        BoardEntity trackBoard = trackBoard("board-track", 10L, "spotify-track");
        BoardEntity albumBoard = albumBoard("board-album", 20L, "spotify-album");
        when(boardRepository.findBoardsByUserTagName("Favorite", BoardStatus.POST))
                .thenReturn(List.of(trackBoard, albumBoard));
        when(boardUserTagRepository.findNamesByBoardIds(List.of("board-track", "board-album")))
                .thenReturn(List.of(
                        new BoardUserTagNameRow("board-track", "Favorite"),
                        new BoardUserTagNameRow("board-album", "Favorite")
                ));
        when(resolvedRepository.findVisibleBySubjectsWithTag(
                SubjectType.TRACK, Set.of(10L), ResolvedStatus.HIDDEN
        )).thenReturn(List.of());
        when(resolvedRepository.findVisibleBySubjectsWithTag(
                SubjectType.ALBUM, Set.of(20L), ResolvedStatus.HIDDEN
        )).thenReturn(List.of());

        List<BoardView> result = service().getBoardsByUserTagName("Favorite");

        assertThat(result).extracting(board -> board.subject().type())
                .containsExactly(SubjectType.TRACK, SubjectType.ALBUM);
        verify(resolvedRepository).findVisibleBySubjectsWithTag(
                SubjectType.TRACK, Set.of(10L), ResolvedStatus.HIDDEN
        );
        verify(resolvedRepository).findVisibleBySubjectsWithTag(
                SubjectType.ALBUM, Set.of(20L), ResolvedStatus.HIDDEN
        );
    }

    @Test
    void LIKE_정렬은_likeCount_repository_query를_선택한다() {
        when(boardRepository.findByStatusAndAlbum_SpotifyIdOrderByLikeCountDesc(
                eq(BoardStatus.POST), eq("spotify-album"), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        BoardPage result = service().getAlbumBoards("spotify-album", BoardOrderType.LIKE, 0);

        assertThat(result.boards()).isEmpty();
        verify(boardRepository).findByStatusAndAlbum_SpotifyIdOrderByLikeCountDesc(
                eq(BoardStatus.POST), eq("spotify-album"), any(Pageable.class)
        );
        verify(boardUserTagRepository, never()).findNamesByBoardIds(any());
        verify(resolvedRepository, never()).findVisibleBySubjectsWithTag(any(), any(), any());
    }

    @Test
    void withdrawn_author_board_remains_public_with_generic_name() {
        TrackEntity track = org.mockito.Mockito.mock(TrackEntity.class);
        when(track.getTrackId()).thenReturn(10L);
        when(track.getSpotifyId()).thenReturn("spotify-track");
        BoardEntity board = BoardEntity.builder().boardId("board-1").content("original content")
                .status(BoardStatus.POST).likeCount(0).track(track)
                .userEntity(UserEntity.builder().userId(1L).userStatus(UserStatus.WITHDRAWN).build())
                .build();
        when(boardRepository.findByBoardIdAndStatus("board-1", BoardStatus.POST))
                .thenReturn(java.util.Optional.of(board));
        when(boardUserTagRepository.findNamesByBoardIds(List.of("board-1")))
                .thenReturn(List.of());
        when(resolvedRepository.findVisibleBySubjectsWithTag(
                SubjectType.TRACK, Set.of(10L), ResolvedStatus.HIDDEN)).thenReturn(List.of());

        BoardView result = service().getDetail("board-1");
        assertThat(result.content()).isEqualTo("original content");
        assertThat(result.nickname()).isEqualTo("탈퇴한 사용자");
    }

    private BoardQueryService service() {
        return new BoardQueryService(boardRepository, boardUserTagRepository, resolvedRepository);
    }

    private BoardEntity trackBoard(String boardId, long trackId, String spotifyId) {
        TrackEntity track = org.mockito.Mockito.mock(TrackEntity.class);
        when(track.getTrackId()).thenReturn(trackId);
        when(track.getSpotifyId()).thenReturn(spotifyId);
        return baseBoard(boardId).track(track).build();
    }

    private BoardEntity albumBoard(String boardId, long albumId, String spotifyId) {
        AlbumEntity album = org.mockito.Mockito.mock(AlbumEntity.class);
        when(album.getAlbumId()).thenReturn(albumId);
        when(album.getSpotifyId()).thenReturn(spotifyId);
        return baseBoard(boardId).album(album).build();
    }

    private BoardEntity.BoardEntityBuilder baseBoard(String boardId) {
        return BoardEntity.builder()
                .boardId(boardId)
                .content("content")
                .status(BoardStatus.POST)
                .likeCount(0)
                .userEntity(UserEntity.builder().userId(1L).nickname("writer").build());
    }

    private SubjectTagResolvedEntity resolved(
            SubjectType type, long subjectId, long tagId, String name, double score
    ) {
        TagEntity tag = org.mockito.Mockito.mock(TagEntity.class);
        when(tag.getTagId()).thenReturn(tagId);
        when(tag.getName()).thenReturn(name);
        SubjectTagResolvedEntity resolved = org.mockito.Mockito.mock(SubjectTagResolvedEntity.class);
        when(resolved.subjectRef()).thenReturn(new SubjectRef(type, subjectId));
        when(resolved.getTag()).thenReturn(tag);
        when(resolved.getScore()).thenReturn(score);
        return resolved;
    }
}

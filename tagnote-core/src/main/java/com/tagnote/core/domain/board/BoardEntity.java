package com.tagnote.core.domain.board;

import com.tagnote.core.domain.BaseTime;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.UuidGenerator;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "board", indexes = {
        @Index(name = "idx_board_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_board_track_created", columnList = "track_id,created_at"),
        @Index(name = "idx_board_album_created", columnList = "album_id,created_at"),
        @Index(name = "idx_board_status_created", columnList = "status,created_at"),
        @Index(name = "idx_board_like_count", columnList = "like_count")
})
@Check(name = "ck_board_exactly_one_subject", constraints =
        "(track_id is not null and album_id is null) or (track_id is null and album_id is not null)")
public class BoardEntity extends BaseTime {
    @Id
    @GeneratedValue(generator = "uuid2")
    @UuidGenerator
    private String boardId;

    @Column(length = 16_777_216)
    private String content;

    @Column(columnDefinition = "VARCHAR(255)")
    @Enumerated(EnumType.STRING)
    private BoardStatus status;

    @ColumnDefault("0")
    private Integer count;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private TrackEntity track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private AlbumEntity album;

    private Integer likeCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    /*
     * 비즈니스 로직
     */
    public static BoardEntity createForTrack(String content, UserEntity writer, TrackEntity track) {
        BoardEntity board = builder()
                .content(content)
                .status(BoardStatus.POST)
                .count(0)
                .likeCount(0)
                .userEntity(writer)
                .track(track)
                .build();
        board.validateSubject();
        return board;
    }

    public static BoardEntity createForAlbum(String content, UserEntity writer, AlbumEntity album) {
        BoardEntity board = builder()
                .content(content)
                .status(BoardStatus.POST)
                .count(0)
                .likeCount(0)
                .userEntity(writer)
                .album(album)
                .build();
        board.validateSubject();
        return board;
    }

    public BoardEntity update(String content) {
        this.content = content;
        return this;
    }

    public void delete() {
        this.status = BoardStatus.REMOVAL;
    }

    /*
     * 서비스 도메인 형변환
     */
    public Board toBoard() {
        return Board.builder()
                .boardId(this.getBoardId())
                .content(this.getContent())
                .status(this.getStatus())
                .count(this.getCount())
                .likeCount(this.getLikeCount())
                .trackId(this.getTrack() == null ? null : this.getTrack().getSpotifyId())
                .albumId(this.getAlbum() == null ? null : this.getAlbum().getSpotifyId())
                .user(this.getUserEntity().toUser())
                .createdAt(this.getCreatedAt())
                .build();
    }

    private void validateSubject() {
        if ((track == null) == (album == null)) {
            throw new IllegalArgumentException("Board must target exactly one Track or Album");
        }
    }
}

package com.tagstory.core.domain.board;

import com.tagstory.core.domain.BaseTime;
import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.like.LikeEntity;
import com.tagstory.core.domain.user.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Table(name = "board")
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

    private String trackId;

    private Integer likeCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
    private List<BoardHashtagEntity> boardHashtagEntityList = new ArrayList<>();

    /*
     * 연관 관계 설정
     */
    public void addUser(UserEntity userEntity) {
        this.userEntity = userEntity;
        if(userEntity.getBoardList() != null) {
            userEntity.addBoard(this);
        }
    }

    public void addBoardHashTagList(List<BoardHashtagEntity> boardHashtagEntityList) {
        this.boardHashtagEntityList = boardHashtagEntityList;
        boardHashtagEntityList.forEach(entity -> entity.addBoard(this));
    }

    /*
     * 비즈니스 로직
     */
    public static BoardEntity create(String content, String trackId) {
        return builder()
                .content(content)
                .status(BoardStatus.POST)
                .count(0)
                .likeCount(0)
                .trackId(trackId)
                .build();
    }

    public BoardEntity update(String content) {
        this.content = content;
        return this;
    }

    public BoardEntity update(String content, List<BoardHashtagEntity> boardHashtagEntityList) {
        this.content = content;
        this.boardHashtagEntityList = boardHashtagEntityList;
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
                .trackId(this.getTrackId())
                .boardHashtagList(this.getBoardHashtagEntityList().stream().map(BoardHashtagEntity::toBoardHashtag).toList())
                .user(this.getUserEntity().toUser())
                .createdAt(this.getCreatedAt())
                .build();
    }
}

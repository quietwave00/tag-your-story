package com.tagstory.core.domain.board;

import com.tagstory.core.domain.BaseTime;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.like.LikeEntity;
import com.tagstory.core.domain.user.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
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
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private String boardId;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    private BoardStatus status;

    @ColumnDefault("0")
    private Integer count;

    private String trackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
    private List<FileEntity> fileList = new ArrayList<>();

    @OneToMany(mappedBy = "board")
    private List<LikeEntity> likeList = new ArrayList<>();


    /*
     * 연관 관계 설정
     */
    public void addUser(UserEntity user) {
        this.user = user;
        if(user.getBoardList() != null) {
            user.addBoard(this);
        }
    }

//    public void addHashtag(List<BoardHashtagEntity> boardHashtagList) {
//        for (BoardHashtagEntity boardHashtag : boardHashtagList) {
//            if(!this.hashtagList.contains(boardHashtag)) {
//                this.hashtagList.add(boardHashtag);
//                boardHashtag.add(this);
//            }
//        }
//    }

    public void addFile(List<FileEntity> fileList) {
        this.fileList = fileList;
    }

    /*
     * 비즈니스 로직
     */
    public static BoardEntity create(CreateBoardCommand createBoardCommand) {
        return BoardEntity.builder()
                .content(createBoardCommand.getContent())
                .status(BoardStatus.POST)
                .count(0)
                .trackId(createBoardCommand.getTrackId())
                .build();
    }

    /*
     * 테스트용 생성자
     */
    public void generateTestBoard(String boardId, String content, String nickname) {
        this.boardId = boardId;
        this.content = content;
        this.user.updateNickname(nickname);
    }
}

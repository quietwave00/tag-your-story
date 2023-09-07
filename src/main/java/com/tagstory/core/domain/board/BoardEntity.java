package com.tagstory.core.domain.board;

import com.tagstory.core.domain.BaseTime;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.user.UserEntity;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class BoardEntity extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardId;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    private BoardStatus status;

    @ColumnDefault("0")
    private Integer count;

    private String trackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    @Builder.Default
    @OneToMany(mappedBy = "boardEntity", cascade = CascadeType.ALL)
    private List<HashtagEntity> hashtagEntityList = new ArrayList<>();


    /*
     * 연관 관계 설정
     */
    public void addUser(UserEntity userEntity) {
        this.userEntity = userEntity;
        if(userEntity.getBoardEntityList() != null) {
            userEntity.getBoardEntityList().add(this);
        }
    }

    public void addHashtag(List<HashtagEntity> hashtagEntityList) {
        this.hashtagEntityList = hashtagEntityList;
        for (HashtagEntity hashtagEntity : hashtagEntityList) {
            hashtagEntity.addBoard(this);
        }
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
    public void generateTestBoard(Long boardId, String content, String nickname) {
        this.boardId = boardId;
        this.content = content;
        this.userEntity.updateNickname(nickname);
    }
}

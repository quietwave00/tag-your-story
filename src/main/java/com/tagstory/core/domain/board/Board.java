package com.tagstory.core.domain.board;

import com.tagstory.core.domain.BaseTime;
import com.tagstory.core.domain.board.dto.receive.ReceiveCreateBoard;
import com.tagstory.core.domain.hashtag.Hashtag;
import com.tagstory.core.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class Board extends BaseTime {
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
    private User user;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL)
    private List<Hashtag> hashtagList = new ArrayList<>();


    /*
     * 연관 관계 설정
     */
    public void addUser(User user) {
        this.user = user;
        if(user.getBoardList() != null) {
            user.getBoardList().add(this);
        }
    }

    public void addHashtag(List<Hashtag> hashtagList) {
        this.hashtagList = hashtagList;
        for (Hashtag hashtag : hashtagList) {
            hashtag.addBoard(this);
        }
    }

    /*
     * 비즈니스 로직
     */
    public static Board create(ReceiveCreateBoard receiveCreateBoard) {
        return Board.builder()
                .content(receiveCreateBoard.getContent())
                .status(BoardStatus.POST)
                .count(0)
                .trackId(receiveCreateBoard.getTrackId())
                .build();
    }
}

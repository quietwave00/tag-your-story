package com.tagstory.core.domain.hashtag;

import com.tagstory.core.domain.board.BoardEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class HashtagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hashtagId;

    private String name;

    @ManyToOne
    @JoinColumn(name = "board_id")
    private BoardEntity boardEntity;


    /*
     * 연관관계 설절
     */
    public void addBoard(BoardEntity boardEntity) {
        this.boardEntity = boardEntity;
    }

    /*
     * 비즈니스 로직
     */
    public static HashtagEntity create(String name) {
        return HashtagEntity.builder()
                .name(name)
                .build();
    }
}

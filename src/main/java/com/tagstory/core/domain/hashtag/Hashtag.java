package com.tagstory.core.domain.hashtag;

import com.tagstory.core.domain.board.Board;
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
public class Hashtag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hashtagId;

    private String name;

    @ManyToOne
    @JoinColumn(name = "board_id")
    private Board board;


    /*
     * 연관관계 설절
     */
    public void addBoard(Board board) {
        this.board = board;
    }

    /*
     * 비즈니스 로직
     */
    public static Hashtag create(String name) {
        return Hashtag.builder()
                .name(name)
                .build();
    }
}

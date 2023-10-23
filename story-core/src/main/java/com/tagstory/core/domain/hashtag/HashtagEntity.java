package com.tagstory.core.domain.hashtag;

import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.hashtag.service.dto.Hashtag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "hashtag",
        indexes = {
                @Index(name = "idx_name", columnList = "name")
        }
)
public class HashtagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hashtagId;

    private String name;

    @OneToMany(mappedBy = "hashtag")
    private List<BoardHashtagEntity> boardHashtagEntityList = new ArrayList<>();

    /*
     * 형변환
     */
    public Hashtag toHashtag() {
        return Hashtag.builder()
                .hashtagId(this.getHashtagId())
                .name(this.getName())
                .build();
    }


    /*
     * 비즈니스 로직
     */
    public static HashtagEntity create(String name) {
        return builder()
                .name(name)
                .build();
    }

    public void addBoardHashTag(BoardHashtagEntity boardHashtagEntity) {
        this.boardHashtagEntityList.add(boardHashtagEntity);
        if(boardHashtagEntity.getHashtag() != this) {
            boardHashtagEntity.addHashTag(this);
        }
    }
}

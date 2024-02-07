package com.tagstory.core.domain.hashtag;

import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.hashtag.service.Hashtag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
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
}

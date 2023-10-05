package com.tagstory.core.domain.hashtag;

import com.tagstory.core.domain.hashtag.service.dto.Hashtag;
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
@Table(name = "hashtag")
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
        return HashtagEntity.builder()
                .name(name)
                .build();
    }
}

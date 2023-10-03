package com.tagstory.core.domain.hashtag;

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
     * 비즈니스 로직
     */
    public static HashtagEntity create(String name) {
        return HashtagEntity.builder()
                .name(name)
                .build();
    }
}

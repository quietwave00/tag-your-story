package com.tagnote.core.domain.usertag;

import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.usertag.service.UserTag;
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
        name = "user_tag",
        indexes = {
                @Index(name = "idx_name", columnList = "name")
        }
)
public class UserTagEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userTagId;

    private String name;

    /*
     * 형변환
     */
    public UserTag toUserTag() {
        return UserTag.builder()
                .userTagId(this.getUserTagId())
                .name(this.getName())
                .build();
    }


    /*
     * 비즈니스 로직
     */
    public static UserTagEntity create(String name) {
        return builder()
                .name(name)
                .build();
    }
}

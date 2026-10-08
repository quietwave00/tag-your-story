package com.tagnote.core.domain.usertag;

import com.tagnote.core.domain.BaseTime;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.usertag.service.UserTag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_tag",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_tag_owner_name",
                columnNames = {"user_id", "name"}
        ),
        indexes = {
                @Index(name = "idx_user_tag_name_owner", columnList = "name,user_id")
        }
)
public class UserTagEntity extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userTagId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false)
    private String name;

    /*
     * 형변환
     */
    public UserTag toUserTag() {
        return UserTag.builder()
                .userTagId(this.getUserTagId())
                .ownerUserId(this.getOwner().getUserId())
                .name(this.getName())
                .build();
    }


    /*
     * 비즈니스 로직
     */
    public static UserTagEntity create(UserEntity owner, String name) {
        return builder()
                .owner(owner)
                .name(name)
                .build();
    }
}

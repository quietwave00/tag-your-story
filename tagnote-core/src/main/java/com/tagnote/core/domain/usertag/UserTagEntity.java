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
                name = "uk_user_tag_owner_normalized_name",
                columnNames = {"user_id", "normalized_name"}
        ),
        indexes = {
                @Index(name = "idx_user_tag_normalized_owner", columnList = "normalized_name,user_id")
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

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    /*
     * 형변환
     */
    public UserTag toUserTag() {
        return UserTag.builder()
                .userTagId(this.getUserTagId())
                .ownerUserId(this.getOwner().getUserId())
                .name(this.getName())
                .normalizedName(this.getNormalizedName())
                .build();
    }


    /*
     * 비즈니스 로직
     */
    public static UserTagEntity create(UserEntity owner, String name, String normalizedName) {
        return builder()
                .owner(owner)
                .name(name)
                .normalizedName(normalizedName)
                .build();
    }
}

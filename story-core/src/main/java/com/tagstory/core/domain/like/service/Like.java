package com.tagstory.core.domain.like.service;

import com.tagstory.core.domain.board.service.Board;
import com.tagstory.core.domain.like.LikeEntity;
import com.tagstory.core.domain.user.service.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Like {
    private Long likeId;

    private User user;

    private Board board;

    /*
     * 락을 걸 시 사용할 키 이름을 반환한다.
     */
    private static final String LOCK_NAME = "like:";
    public static String getLockNameOfKey(Long likeId) {
        return LOCK_NAME + likeId;
    }

    /*
     * 형변환
     */
    public LikeEntity toEntity() {
        return LikeEntity.builder()
                .likeId(this.likeId)
                .user(this.user.toEntity())
                .board(this.board.toEntity())
                .build();
    }
}

package com.tagstory.domain.board.fixture;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.user.UserEntity;

public class BoardFixture {
    public static BoardEntity createBoardEntity() {
        return  BoardEntity.builder()
                .boardId("1")
                .build();
    }

    public static BoardEntity createBoardEntityWithUser() {
        BoardEntity boardEntity = createBoardEntity();
        UserEntity userEntity = UserEntity.builder()
                .userId(1L)
                .build();
        boardEntity.addUser(userEntity);
        return boardEntity;
    }
}

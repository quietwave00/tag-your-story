package com.tagnote.domain.board.fixture;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.service.User;

public class BoardFixture {
    public static BoardEntity createBoardEntity() {
        return  BoardEntity.builder()
                .boardId("1")
                .build();
    }

    public static BoardEntity createBoardEntityWithUserEntity() {
        UserEntity userEntity = UserEntity.builder()
                .userId(1L)
                .build();
        return BoardEntity.builder().boardId("1").userEntity(userEntity).build();
    }

    public static Board createBoard(String boardId) {
        return Board.builder()
                .boardId(boardId)
                .build();
    }

    public static Board createBoardWithUser(String boardId, Long userId) {
        return Board.builder()
                .boardId(boardId)
                .user(User.builder().userId(userId).build())
                .build();
    }
}

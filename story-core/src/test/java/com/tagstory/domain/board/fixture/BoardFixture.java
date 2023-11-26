package com.tagstory.domain.board.fixture;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.service.dto.response.User;

public class BoardFixture {
    public static BoardEntity createBoardEntity() {
        return  BoardEntity.builder()
                .boardId("1")
                .build();
    }

    public static BoardEntity createBoardEntityWithUserEntity() {
        BoardEntity boardEntity = createBoardEntity();
        UserEntity userEntity = UserEntity.builder()
                .userId(1L)
                .build();
        boardEntity.addUser(userEntity);
        return boardEntity;
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

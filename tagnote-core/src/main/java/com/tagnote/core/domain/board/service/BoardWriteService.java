package com.tagnote.core.domain.board.service;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import com.tagnote.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.service.BoardUserTagService;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.service.UserTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardWriteService {

    private final BoardService boardService;
    private final UserService userService;
    private final UserTagService userTagService;
    private final BoardUserTagService boardUserTagService;

    @Transactional
    public Board create(CreateBoardCommand command) {
        User user = userService.getCacheByUserId(command.getUserId());
        UserEntity owner = user.toEntity();
        BoardEntity boardEntity = BoardEntity.create(command.getContent(), command.getTrackId());
        boardEntity.addUser(owner);

        List<UserTagEntity> userTags = userTagService.makeUserTagList(owner, command.getUserTagList());
        List<BoardUserTagEntity> boardUserTags = boardUserTagService.makeBoardUserTagList(boardEntity, userTags);
        return boardService.create(boardEntity, boardUserTags);
    }

    @Transactional
    public Board updateBoardAndUserTag(UpdateBoardCommand command) {
        BoardEntity boardEntity = boardService.getBoardEntityByBoardId(command.getBoardId());
        if (command.getUserTagList().isEmpty()) {
            return boardService.updateBoard(command, boardEntity);
        }

        List<UserTagEntity> userTags = userTagService.makeUserTagList(
                boardEntity.getUserEntity(), command.getUserTagList()
        );
        List<BoardUserTagEntity> boardUserTags = boardUserTagService.makeBoardUserTagList(boardEntity, userTags);
        boardUserTagService.deleteUserTag(command.getBoardId());
        return boardService.updateBoardWithUserTag(command, boardEntity, boardUserTags);
    }
}

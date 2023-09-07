package com.tagstory.core.domain.board.service;

import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.response.BoardByTrack;
import com.tagstory.core.domain.board.dto.response.CreateBoard;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.hashtag.service.HashtagService;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardFacade {

    private final BoardService boardService;
    private final UserService userService;
    private final HashtagService hashtagService;

    public CreateBoard create(CreateBoardCommand createBoardCommand) {
        UserEntity findUserEntity = userService.findByUserId(createBoardCommand.getUserId());
        List<HashtagEntity> hashtagEntityList = hashtagService.getHashtagList(createBoardCommand.getHashtagList());
        return boardService.create(createBoardCommand, findUserEntity, hashtagEntityList);
    }

    public List<BoardByTrack> getBoardListByTrackId(String trackId) {
        return boardService.getBoardListByTrackId(trackId);
    }
}

package com.tagstory.core.domain.board.service;

import com.tagstory.core.domain.board.dto.receive.ReceiveCreateBoard;
import com.tagstory.core.domain.board.dto.response.CreateBoardResponse;
import com.tagstory.core.domain.hashtag.Hashtag;
import com.tagstory.core.domain.hashtag.service.HashtagService;
import com.tagstory.core.domain.user.User;
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

    public CreateBoardResponse create(ReceiveCreateBoard receiveCreateBoard, Long userId) {
        User findUser = userService.findByUserId(userId);
        List<Hashtag> hashtagList = hashtagService.getHashtagList(receiveCreateBoard.getHashtagList());
        return boardService.create(receiveCreateBoard, findUser, hashtagList);
    }
}

package com.tagstory.core.domain.board.service;

import com.tagstory.core.domain.board.Board;
import com.tagstory.core.domain.board.dto.receive.ReceiveCreateBoard;
import com.tagstory.core.domain.board.dto.response.CreateBoardResponse;
import com.tagstory.core.domain.board.repository.BoardRepository;
import com.tagstory.core.domain.hashtag.Hashtag;
import com.tagstory.core.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    @Transactional
    public CreateBoardResponse create(ReceiveCreateBoard receiveCreateBoard, User user, List<Hashtag> hashtagList) {
        Board beforeBoard = Board.create(receiveCreateBoard);
        beforeBoard.addUser(user);
        beforeBoard.addHashtag(hashtagList);
        Board savedBoard = boardRepository.save(beforeBoard);
        return CreateBoardResponse.onComplete(savedBoard);
    }
}

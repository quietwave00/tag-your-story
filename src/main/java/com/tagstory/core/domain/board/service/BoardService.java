package com.tagstory.core.domain.board.service;

import com.tagstory.core.domain.board.Board;
import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.response.BoardByTrackResponse;
import com.tagstory.core.domain.board.dto.response.CreateBoardResponse;
import com.tagstory.core.domain.board.repository.BoardRepository;
import com.tagstory.core.domain.hashtag.Hashtag;
import com.tagstory.core.domain.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    @Transactional
    public CreateBoardResponse create(CreateBoardCommand createBoardCommand, User user, List<Hashtag> hashtagList) {
        Board beforeBoard = Board.create(createBoardCommand);
        beforeBoard.addUser(user);
        beforeBoard.addHashtag(hashtagList);
        Board savedBoard = boardRepository.save(beforeBoard);
        return CreateBoardResponse.onComplete(savedBoard);
    }

    public List<BoardByTrackResponse> getBoardListByTrackId(String trackId) {
        List<Board> boardList = boardRepository.findByStatusAndTrackIdOrderByBoardIdDesc(BoardStatus.POST, trackId);
        return boardList.stream()
                .map(BoardByTrackResponse::onComplete)
                .collect(Collectors.toList());
    }
}

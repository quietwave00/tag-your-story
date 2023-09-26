package com.tagstory.core.domain.board.service;

import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.response.BoardByTrack;
import com.tagstory.core.domain.board.dto.response.CreateBoard;
import com.tagstory.core.domain.board.dto.response.DetailBoard;
import com.tagstory.core.domain.board.repository.BoardRepository;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.boardhashtag.repository.BoardHashtagRepository;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.user.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardHashtagRepository boardHashtagRepository;

    @Transactional
    public CreateBoard create(CreateBoardCommand createBoardCommand, UserEntity user, List<HashtagEntity> hashtagList) {
        BoardEntity board = BoardEntity.create(createBoardCommand);
        board.addUser(user);
        BoardEntity savedBoard = boardRepository.save(board);
        hashtagList.stream()
                .map(hashtag -> BoardHashtagEntity.of(board, hashtag))
                .forEach(boardHashtagRepository::save);
        return CreateBoard.onComplete(savedBoard, hashtagList);
    }

    public List<BoardByTrack> getBoardListByTrackId(Page<BoardEntity> boardList, List<HashtagNameList> hashtagNameListByBoard) {
        return boardList.stream()
                .flatMap(board -> hashtagNameListByBoard.stream()
                .map(hashtagNameList -> BoardByTrack.onComplete(board, hashtagNameList)))
                .collect(Collectors.toList());
    }

    public DetailBoard getDetailBoard(String boardId, HashtagNameList hashtagNameList) {
        BoardEntity board = boardRepository.findByBoardId(boardId);
        return DetailBoard.onComplete(board, hashtagNameList);
    }

    public BoardEntity findByBoardId(String boardId) {
        return boardRepository.findByBoardIdAndStatus(boardId, BoardStatus.POST).orElseThrow(() -> new CustomException(ExceptionCode.BOARD_NOT_FOUND));
    }

    public List<BoardEntity> findByTrackId(String trackId) {
        return boardRepository.findByTrackId(trackId);
    }

    public Page<BoardEntity> findBoardByStatusAndTrackId(String trackId, int page) {
        return boardRepository.findByStatusAndTrackIdOrderByBoardIdDesc(BoardStatus.POST, trackId, PageRequest.of(page, 8));
    }
}

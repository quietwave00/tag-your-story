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
import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.user.UserEntity;
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
    public CreateBoard create(CreateBoardCommand createBoardCommand, UserEntity userEntity, List<HashtagEntity> hashtagEntityList) {
        BoardEntity beforeBoardEntity = BoardEntity.create(createBoardCommand);
        beforeBoardEntity.addUser(userEntity);
        beforeBoardEntity.addHashtag(hashtagEntityList);
        BoardEntity savedBoardEntity = boardRepository.save(beforeBoardEntity);
        return CreateBoard.onComplete(savedBoardEntity);
    }

    public List<BoardByTrack> getBoardListByTrackId(String trackId) {
        List<BoardEntity> boardEntityList = boardRepository.findByStatusAndTrackIdOrderByBoardIdDesc(BoardStatus.POST, trackId);
        return boardEntityList.stream()
                .map(BoardByTrack::onComplete)
                .collect(Collectors.toList());
    }

    public DetailBoard getDetailBoard(Long boardId) {
        BoardEntity board = findByBoardId(boardId);
        return DetailBoard.onComplete(board);
    }

    public BoardEntity findByBoardId(Long boardId) {
        return boardRepository.findByBoardIdAndStatus(boardId, BoardStatus.POST).orElseThrow(() -> new CustomException(ExceptionCode.BOARD_NOT_FOUND));
    }

    public BoardEntity getReferenceById(Long boardId) {
        return boardRepository.getReferenceById(boardId);
    }

    public List<BoardEntity> findByTrackId(String trackId) {
        return boardRepository.findByTrackId(trackId);
    }
}

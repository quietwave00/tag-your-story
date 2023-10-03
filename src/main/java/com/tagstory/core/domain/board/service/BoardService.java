package com.tagstory.core.domain.board.service;

import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.board.repository.BoardRepository;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.boardhashtag.repository.BoardHashtagRepository;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardHashtagRepository boardHashtagRepository;

    @Transactional
    public Board create(CreateBoardCommand command, User user, List<HashtagEntity> hashtagEntityList) {
        BoardEntity boardEntity = BoardEntity.create(command);
        boardEntity.addUser(user.toEntity());
        BoardEntity savedBoard = boardRepository.save(boardEntity);

        /* 해시태그 중간 테이블에 저장 */
        hashtagEntityList.stream()
                .map(hashtagEntity -> BoardHashtagEntity.of(boardEntity, hashtagEntity))
                .forEach(boardHashtagRepository::save);

        return savedBoard.toBoard();
    }

    public List<Board> getBoardListByTrackId(List<Board> boardList, List<HashtagNameList> hashtagNameListByBoardList) {
        return IntStream.range(0, boardList.size())
                .mapToObj(i -> boardList.get(i).addHashtagList(hashtagNameListByBoardList.get(i)))
                .collect(Collectors.toList());
    }

    public Board getDetailBoard(String boardId, HashtagNameList hashtagNameList) {
        Board board = getBoardByBoardId(boardId);
        return board.addHashtagList(hashtagNameList);
    }


    /*
     * 단일 메소드
     */
    public Board getBoardByBoardId(String boardId) {
        return boardRepository.findByBoardIdAndStatus(boardId, BoardStatus.POST).orElseThrow(() -> new CustomException(ExceptionCode.BOARD_NOT_FOUND)).toBoard();
    }

    public List<Board> findByTrackId(String trackId) {
        List<BoardEntity> boardEntityList = boardRepository.findByTrackId(trackId);

        return Optional.ofNullable(boardEntityList)
                .map(entityList -> entityList.stream().map(BoardEntity::toBoard).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public List<Board> getBoardListByStatusAndTrackId(BoardStatus status, String trackId, int page) {
        Page<BoardEntity> boardEntityPage = boardRepository.findByStatusAndTrackIdOrderByBoardIdDesc(status, trackId, PageRequest.of(page, 8));

        return boardEntityPage.getContent().stream()
                .map(BoardEntity::toBoard)
                .collect(Collectors.toList());
    }
}
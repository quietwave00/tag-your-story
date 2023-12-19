package com.tagstory.core.domain.board.service;

import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.board.repository.BoardRepository;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.boardhashtag.repository.BoardHashtagRepository;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardHashtagRepository boardHashtagRepository;

    @Transactional
    public Board create(BoardEntity boardEntity, User user, List<BoardHashtagEntity> boardHashtagEntityList) {
        boardEntity.addUser(user.toEntity());
        boardEntity.addBoardHashTagList(boardHashtagEntityList);
        BoardEntity savedBoard = boardRepository.save(boardEntity);
        HashtagNameList hashtagNameList = getHashtagNameListByBoardId(savedBoard.getBoardId());
        return savedBoard.toBoard().addHashtagList(hashtagNameList);
    }

    public List<Board> getBoardListByTrackId(List<Board> boardList, List<HashtagNameList> hashtagNameListByBoardList) {
        return IntStream.range(0, boardList.size())
                .mapToObj(i -> boardList.get(i).addHashtagList(hashtagNameListByBoardList.get(i)))
                .collect(Collectors.toList());
    }

    public List<Board> getBoardListByTrackIdSortedLike(BoardStatus status, String trackId, int page) {
        Page<BoardEntity> boardEntityPage = boardRepository.
                findByStatusAndTrackIdOrderByLikeCountDesc(status, trackId, PageRequest.of(page, 8));

        return boardEntityPage.getContent().stream()
                .map(BoardEntity::toBoard)
                .collect(Collectors.toList());
    }

    public Board getDetailBoard(String boardId, HashtagNameList hashtagNameList) {
        Board board = getBoardByBoardId(boardId);
        return board.addHashtagList(hashtagNameList);
    }

    public int getBoardCountByTrackId(String trackId) {
        return boardRepository.countByTrackIdAndStatus(trackId, BoardStatus.POST);
    }

    public Boolean isWriter(String boardId, Long userId) {
        Board board = findBoardByBoardIdAndUserId(boardId, userId);
        return Objects.nonNull(board);
    }

    @Transactional
    public Board updateBoard(UpdateBoardCommand command, BoardEntity boardEntity) {
        return boardEntity.update(command.getContent()).toBoard();
    }

    @Transactional
    public Board updateBoardWithHashtag(UpdateBoardCommand command,
                                        BoardEntity boardEntity,
                                        List<BoardHashtagEntity> boardHashtagEntityList) {
        return boardEntity.update(command.getContent(), boardHashtagEntityList).toBoard();
    }

    @Transactional
    public void delete(String boardId) {
        try {
            BoardEntity boardEntity = getBoardEntityByBoardId(boardId);
            boardEntity.delete();
        } catch(Exception e) {
            throw new RuntimeException("An exception occurred While deleting the board.");
        }
    }

    @Transactional
    public void increaseLikeCount(String boardId) {
        boardRepository.updateLikeCount(boardId, 1);
    }

    @Transactional
    public void decreaseLikeCount(String boardId) {
        boardRepository.updateLikeCount(boardId, -1);
    }

    /*
     * 단일 메소드
     */
    public Board getBoardByBoardId(String boardId) {
        return boardRepository.findByBoardIdAndStatus(boardId, BoardStatus.POST)
                .orElseThrow(() -> new CustomException(ExceptionCode.BOARD_NOT_FOUND)).toBoard();
    }

    public BoardEntity getBoardEntityByBoardId(String boardId) {
        return boardRepository.findByBoardIdAndStatus(boardId, BoardStatus.POST)
                .orElseThrow(() -> new CustomException(ExceptionCode.BOARD_NOT_FOUND));
    }

    public List<Board> getBoardListByTrackIdSortedCreatedAt(BoardStatus status, String trackId, int page) {
        Page<BoardEntity> boardEntityPage = boardRepository.
                findByStatusAndTrackIdOrderByCreatedAtDesc(status, trackId, PageRequest.of(page, 8));

        return boardEntityPage.getContent().stream()
                .map(BoardEntity::toBoard)
                .collect(Collectors.toList());
    }

    public List<Board> findByTrackId(String trackId, int page) {
        Page<BoardEntity> boardEntityList = boardRepository.findByTrackId(trackId, PageRequest.of(page, 8));

        return Optional.ofNullable(boardEntityList)
                .map(entityList -> entityList.stream().map(BoardEntity::toBoard).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public List<Board> getBoardListByHashtagId(Long hashtagId) {
        return boardRepository.findBoardsByHashtagId(hashtagId).stream().map(BoardEntity::toBoard).collect(Collectors.toList());
    }

    public HashtagNameList getHashtagNameListByBoardId(String boardId) {
        List<String> hashtagName = boardHashtagRepository.findHashtagNameByBoardId(boardId);
        return HashtagNameList.onComplete(hashtagName);
    }

    @Nullable
    private Board findBoardByBoardIdAndUserId(String boardId, Long userId) {
        Optional<BoardEntity> boardEntityOptional = boardRepository.findByBoardIdAndUserEntity_UserId(boardId, userId);
        return boardEntityOptional.map(BoardEntity::toBoard).orElse(null);
    }
}
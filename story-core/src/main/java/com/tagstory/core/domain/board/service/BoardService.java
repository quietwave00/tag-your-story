package com.tagstory.core.domain.board.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagstory.core.domain.board.repository.BoardRepository;
import com.tagstory.core.domain.board.service.dto.BoardList;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.boardhashtag.repository.BoardHashtagRepository;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNames;
import com.tagstory.core.domain.user.service.User;
import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.annotation.Nullable;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardHashtagRepository boardHashtagRepository;

    public Board create(BoardEntity boardEntity, User user, List<BoardHashtagEntity> boardHashtagEntityList, CreateBoardCommand command) {
        boardEntity.addUser(user.toEntity());
        boardEntity.addBoardHashTagList(boardHashtagEntityList);
        BoardEntity savedBoard = boardRepository.save(boardEntity);

        return savedBoard.toBoard().addHashtagList(HashtagNames.ofNameList(command.getHashtagList()));
    }

    public BoardList getBoardListByTrackId(BoardList boardListResponse, List<HashtagNames> hashtagNameListByBoardList) {
        List<Board> pagedBoardList = boardListResponse.getBoardList();
        List<Board> boardList = new ArrayList<>();

        for (int i = 0; i < pagedBoardList.size(); i++) {
            Board board = pagedBoardList.get(i);
            HashtagNames hashtagNameList = hashtagNameListByBoardList.get(i);
            boardList.add(board.addHashtagList(hashtagNameList));
        }

        return BoardList.of(boardList, boardListResponse.getTotalCount());
    }

    public Board getDetailBoard(String boardId, HashtagNames hashtagNameList) {
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

    public Board updateBoard(UpdateBoardCommand command, BoardEntity boardEntity) {
        return boardEntity.update(command.getContent()).toBoard();
    }

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

    public BoardList getBoardListByTrackIdSortedCreatedAt(BoardStatus status, String trackId, int page) {
        Page<BoardEntity> boardEntityPage = boardRepository.
                findByStatusAndTrackIdOrderByCreatedAtDesc(status, trackId, PageRequest.of(page, 8));

        long totalCount = boardEntityPage.getTotalElements();
        List<Board> boardList = boardEntityPage.getContent().stream()
                .map(BoardEntity::toBoard)
                .toList();

        return BoardList.of(boardList, totalCount);
    }

    public BoardList getBoardListByTrackIdSortedLike(BoardStatus status, String trackId, int page) {
        Page<BoardEntity> boardEntityPage = boardRepository.
                findByStatusAndTrackIdOrderByLikeCountDesc(status, trackId, PageRequest.of(page, 8));

        long totalCount = boardEntityPage.getTotalElements();
        List<Board> boardList = boardEntityPage.getContent().stream()
                .map(BoardEntity::toBoard)
                .toList();

        return BoardList.of(boardList, totalCount);
    }

    public List<Board> findByTrackId(String trackId, int page) {
        Page<BoardEntity> boardEntityList = boardRepository.findByTrackId(trackId, PageRequest.of(page, 8));

        return Optional.ofNullable(boardEntityList)
                .map(entityList -> entityList.stream().map(BoardEntity::toBoard).toList())
                .orElse(Collections.emptyList());
    }

    public List<Board> getBoardListByHashtagId(Long hashtagId) {
        return boardRepository.findBoardsByHashtagId(hashtagId).stream().map(BoardEntity::toBoard).toList();
    }

    public HashtagNames getHashtagNameListByBoardId(String boardId) {
        List<String> hashtagName = boardHashtagRepository.findHashtagNameByBoardId(boardId);
        return HashtagNames.ofNameList(hashtagName);
    }

    /*
     * private
     */
    @Nullable
    private Board findBoardByBoardIdAndUserId(String boardId, Long userId) {
        Optional<BoardEntity> boardEntityOptional = boardRepository.findByBoardIdAndUserEntity_UserId(boardId, userId);
        return boardEntityOptional.map(BoardEntity::toBoard).orElse(null);
    }

}
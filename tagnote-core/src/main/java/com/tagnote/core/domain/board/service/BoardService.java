package com.tagnote.core.domain.board.service;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagnote.core.domain.board.repository.BoardRepository;
import com.tagnote.core.domain.board.service.dto.BoardList;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
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
    private final BoardUserTagRepository boardUserTagRepository;

    public Board create(BoardEntity boardEntity, List<BoardUserTagEntity> boardUserTagEntityList) {
        boardEntity.addBoardUserTagList(boardUserTagEntityList);
        BoardEntity savedBoard = boardRepository.save(boardEntity);

        return savedBoard.toBoard().addUserTagList(UserTagNames.ofEntityList(
                boardUserTagEntityList.stream().map(BoardUserTagEntity::getUserTag).toList()
        ));
    }

    public BoardList getBoardListByTrackId(BoardList boardListResponse, List<UserTagNames> userTagNameListByBoardList) {
        List<Board> pagedBoardList = boardListResponse.getBoardList();
        List<Board> boardList = new ArrayList<>();

        for (int i = 0; i < pagedBoardList.size(); i++) {
            Board board = pagedBoardList.get(i);
            UserTagNames userTagNameList = userTagNameListByBoardList.get(i);
            boardList.add(board.addUserTagList(userTagNameList));
        }

        return BoardList.of(boardList, boardListResponse.getTotalCount());
    }

    public Board getDetailBoard(String boardId, UserTagNames userTagNameList) {
        Board board = getBoardByBoardId(boardId);
        return board.addUserTagList(userTagNameList);
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

    public Board updateBoardWithUserTag(UpdateBoardCommand command,
                                        BoardEntity boardEntity,
                                        List<BoardUserTagEntity> boardUserTagEntityList) {
        return boardEntity.update(command.getContent(), boardUserTagEntityList).toBoard();
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

    public List<Board> getBoardListByNormalizedUserTagName(String normalizedName) {
        return boardRepository.findBoardsByNormalizedUserTagName(normalizedName, BoardStatus.POST)
                .stream()
                .map(boardEntity -> boardEntity.toBoard().addUserTagList(UserTagNames.ofEntityList(
                        boardEntity.getBoardUserTagEntityList().stream()
                                .map(BoardUserTagEntity::getUserTag)
                                .toList()
                )))
                .toList();
    }

    public UserTagNames getUserTagNameListByBoardId(String boardId) {
        List<String> userTagName = boardUserTagRepository.findUserTagNameByBoardId(boardId);
        return UserTagNames.ofNameList(userTagName);
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

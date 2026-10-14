package com.tagnote.core.domain.board.service;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.infrastructure.persistence.board.BoardJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Legacy Board reference service retained for Like, Comment and File flows.
 * Board HTTP use cases are handled by application.board services.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardJpaRepository boardRepository;

    public Board getBoardByBoardId(String boardId) {
        return getBoardEntityByBoardId(boardId).toBoard();
    }

    public BoardEntity getBoardEntityByBoardId(String boardId) {
        return boardRepository.findByBoardIdAndStatus(boardId, BoardStatus.POST)
                .orElseThrow(() -> new CustomException(ExceptionCode.BOARD_NOT_FOUND));
    }

    public List<Board> findByTrackId(String spotifyTrackId, int page) {
        Page<BoardEntity> boards = boardRepository.findByTrack_SpotifyId(
                spotifyTrackId, PageRequest.of(page, 8)
        );
        return boards.stream().map(BoardEntity::toBoard).toList();
    }

    @Transactional
    public void increaseLikeCount(String boardId) {
        boardRepository.updateLikeCount(boardId, 1);
    }

    @Transactional
    public void decreaseLikeCount(String boardId) {
        boardRepository.updateLikeCount(boardId, -1);
    }
}

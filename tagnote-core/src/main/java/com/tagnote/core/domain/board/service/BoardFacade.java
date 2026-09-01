package com.tagnote.core.domain.board.service;

import com.tagnote.core.domain.board.BoardOrderType;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import com.tagnote.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagnote.core.domain.board.service.dto.BoardList;
import com.tagnote.core.domain.boardusertag.service.BoardUserTag;
import com.tagnote.core.domain.boardusertag.service.BoardUserTagService;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardFacade {

    private final BoardService boardService;
    private final BoardUserTagService boardUserTagService;
    private final BoardWriteService boardWriteService;

    public Board create(CreateBoardCommand command) {
        return boardWriteService.create(command);
    }

    public BoardList getBoardListByTrackId(String trackId, BoardOrderType orderType, int page) {
        BoardList boardListResponse = orderType.isCreatedAt()
                ? boardService.getBoardListByTrackIdSortedCreatedAt(BoardStatus.POST, trackId, page)
                : boardService.getBoardListByTrackIdSortedLike(BoardStatus.POST, trackId, page);

        List<Board> boardList = boardListResponse.getBoardList();
        List<List<BoardUserTag>> boardUserTagList = boardList.stream().map(Board::getBoardUserTagList).toList();

        List<UserTagNames> userTagNamesList = boardUserTagList.stream().map(boardUserTags -> {
            List<String> userTagNames = boardUserTags.stream()
                    .map(boardUserTag -> boardUserTag.getUserTag().getName())
                    .collect(Collectors.toList());
            return UserTagNames.ofNameList(userTagNames);
        }).toList();

        return boardService.getBoardListByTrackId(boardListResponse, userTagNamesList);
    }

    public Board getDetailBoard(String boardId) {
        UserTagNames userTagNameList = boardUserTagService.getUserTagNameByBoardId(boardId);
        return boardService.getDetailBoard(boardId, userTagNameList);
    }

    public int getBoardCountByTrackId(String trackId) {
        return boardService.getBoardCountByTrackId(trackId);
    }

    public List<Board> getBoardListByUserTagName(String userTagName) {
        List<Board> boards = boardService.getBoardListByUserTagName(userTagName);
        if (boards.isEmpty()) {
            throw new CustomException(ExceptionCode.USER_TAG_NOT_FOUND);
        }
        return boards;
    }

    public Boolean isWriter(String boardId, Long userId) {
        return boardService.isWriter(boardId, userId);
    }

    public Board updateBoardAndUserTag(UpdateBoardCommand command) {
        return boardWriteService.updateBoardAndUserTag(command);
    }

    public void delete(String boardId) {
        boardService.delete(boardId);
    }
}

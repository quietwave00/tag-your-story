package com.tagnote.core.domain.board.service;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardOrderType;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import com.tagnote.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagnote.core.domain.board.service.dto.BoardList;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.service.BoardUserTag;
import com.tagnote.core.domain.boardusertag.service.BoardUserTagService;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.service.UserTagService;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BoardFacade {

    private final BoardService boardService;
    private final UserService userService;
    private final UserTagService userTagService;
    private final BoardUserTagService boardUserTagService;

    @Transactional
    public Board create(CreateBoardCommand command) {
        BoardEntity boardEntity = BoardEntity.create(command.getContent(), command.getTrackId());
        User user = userService.getCacheByUserId(command.getUserId());
        List<UserTagEntity> userTagEntityList = userTagService.makeUserTagList(command.getUserTagList());
        List<BoardUserTagEntity> boardUserTagEntityList = boardUserTagService.makeBoardUserTagList(boardEntity, userTagEntityList);
        return boardService.create(boardEntity, user, boardUserTagEntityList, command);
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
        /* 유저 태그 이름으로부터 아이디값을 찾아 해당하는 아이디를 가진 게시글 리스트를 찾는다. */
        Long userTagId = userTagService.getUserTagIdByUserTagName(userTagName);
        List<Board> beforeBoardList = boardService.getBoardListByUserTagId(userTagId);

        /* Board 객체에 UserTag 리스트를 찾아서 add 해주고 반환한다. */
        return beforeBoardList.stream().peek(board -> {
            board.addUserTagList(boardUserTagService.getUserTagNameByBoardId(board.getBoardId()));
        }).toList();
    }

    public Boolean isWriter(String boardId, Long userId) {
        return boardService.isWriter(boardId, userId);
    }

    @Transactional
    public Board updateBoardAndUserTag(UpdateBoardCommand command) {
        BoardEntity boardEntity = boardService.getBoardEntityByBoardId(command.getBoardId());

        /* 유저 태그에 수정 사항이 있으면 해당 게시글의 유저 태그 모두 삭제 후 요청 값으로 insert */
        if(!command.getUserTagList().isEmpty()) {
            boardUserTagService.deleteUserTag(command.getBoardId());
            List<UserTagEntity> userTagEntityList = userTagService.makeUserTagList(command.getUserTagList());
            List<BoardUserTagEntity> boardUserTagEntityList = boardUserTagService.makeBoardUserTagList(boardEntity, userTagEntityList);

            return boardService.updateBoardWithUserTag(command, boardEntity, boardUserTagEntityList);
        }
        return boardService.updateBoard(command, boardEntity);
    }

    public void delete(String boardId) {
        boardService.delete(boardId);
    }
}

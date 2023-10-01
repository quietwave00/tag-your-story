package com.tagstory.core.domain.board.service;

import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.boardhashtag.service.BoardHashtagService;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.hashtag.service.HashtagService;
import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BoardFacade {

    private final BoardService boardService;
    private final UserService userService;
    private final HashtagService hashtagService;
    private final BoardHashtagService boardHashtagService;

    public Board create(CreateBoardCommand command) {
        User user = userService.getCacheByUserId(command.getUserId());
        List<HashtagEntity> hashtagEntityList = hashtagService.makeHashtagList(command.getHashtagList());
        return boardService.create(command, user, hashtagEntityList);
    }

    public List<Board> getBoardListByTrackId(String trackId, int page) {
        List<Board> boardList = boardService.getBoardListByStatusAndTrackId(BoardStatus.POST, trackId, page);
        List<HashtagNameList> hashtagNameListByBoardList = boardList.stream()
                .map(board -> boardHashtagService.getHashtagName(board.getBoardId()))
                .collect(Collectors.toList());
        return boardService.getBoardListByTrackId(boardList, hashtagNameListByBoardList);
    }

    public Board getDetailBoard(String boardId) {
        HashtagNameList hashtagNameList = boardHashtagService.getHashtagName(boardId);
        return boardService.getDetailBoard(boardId, hashtagNameList);
    }
}

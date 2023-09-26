package com.tagstory.core.domain.board.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.response.BoardByTrack;
import com.tagstory.core.domain.board.dto.response.CreateBoard;
import com.tagstory.core.domain.board.dto.response.DetailBoard;
import com.tagstory.core.domain.boardhashtag.repository.BoardHashtagRepository;
import com.tagstory.core.domain.boardhashtag.service.BoardHashtagService;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.hashtag.service.HashtagService;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BoardFacade {

    private final BoardService boardService;
    private final UserService userService;
    private final HashtagService hashtagService;
    private final BoardHashtagService boardHashtagService;

    public CreateBoard create(CreateBoardCommand createBoardCommand) {
        UserEntity user = userService.findByUserId(createBoardCommand.getUserId());
        List<HashtagEntity> hashtagList = hashtagService.makeHashtagList(createBoardCommand.getHashtagList());
        return boardService.create(createBoardCommand, user, hashtagList);
    }

    public List<BoardByTrack> getBoardListByTrackId(String trackId, int page) {
        Page<BoardEntity> boardList = boardService.findBoardByStatusAndTrackId(trackId, page);
        List<HashtagNameList> hashtagNameListByBoard = boardList.stream()
                .map(board -> boardHashtagService.getHashtagName(board.getBoardId()))
                .collect(Collectors.toList());
        return boardService.getBoardListByTrackId(boardList, hashtagNameListByBoard);
    }

    public DetailBoard getDetailBoard(String boardId) {
        HashtagNameList hashtagNameList = boardHashtagService.getHashtagName(boardId);
        return boardService.getDetailBoard(boardId, hashtagNameList);
    }
}

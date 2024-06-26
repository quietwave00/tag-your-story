package com.tagstory.core.domain.board.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.BoardOrderType;
import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagstory.core.domain.board.service.dto.BoardList;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.boardhashtag.service.BoardHashtag;
import com.tagstory.core.domain.boardhashtag.service.BoardHashtagService;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNames;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.hashtag.service.HashtagService;
import com.tagstory.core.domain.user.service.User;
import com.tagstory.core.domain.user.service.UserService;
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
    private final HashtagService hashtagService;
    private final BoardHashtagService boardHashtagService;

    @Transactional
    public Board create(CreateBoardCommand command) {
        BoardEntity boardEntity = BoardEntity.create(command.getContent(), command.getTrackId());
        User user = userService.getCacheByUserId(command.getUserId());
        List<HashtagEntity> hashtagEntityList = hashtagService.makeHashtagList(command.getHashtagList());
        List<BoardHashtagEntity> boardHashtagEntityList = boardHashtagService.makeBoardHashtagList(boardEntity, hashtagEntityList);
        return boardService.create(boardEntity, user, boardHashtagEntityList, command);
    }

    public BoardList getBoardListByTrackId(String trackId, BoardOrderType orderType, int page) {
        BoardList boardListResponse = orderType.isCreatedAt()
                ? boardService.getBoardListByTrackIdSortedCreatedAt(BoardStatus.POST, trackId, page)
                : boardService.getBoardListByTrackIdSortedLike(BoardStatus.POST, trackId, page);

        List<Board> boardList = boardListResponse.getBoardList();
        List<List<BoardHashtag>> boardHashtagList = boardList.stream().map(Board::getBoardHashtagList).toList();

        List<HashtagNames> hashtagNamesList = boardHashtagList.stream().map(boardHashtags -> {
            List<String> hashtagNames = boardHashtags.stream()
                    .map(boardHashtag -> boardHashtag.getHashtag().getName())
                    .collect(Collectors.toList());
            return HashtagNames.ofNameList(hashtagNames);
        }).toList();

        return boardService.getBoardListByTrackId(boardListResponse, hashtagNamesList);
    }

    public Board getDetailBoard(String boardId) {
        HashtagNames hashtagNameList = boardHashtagService.getHashtagNameByBoardId(boardId);
        return boardService.getDetailBoard(boardId, hashtagNameList);
    }

    public int getBoardCountByTrackId(String trackId) {
        return boardService.getBoardCountByTrackId(trackId);
    }

    public List<Board> getBoardListByHashtagName(String hashtagName) {
        /* 해시태그 이름으로부터 아이디값을 찾아 해당하는 아이디를 가진 게시글 리스트를 찾는다. */
        Long hashtagId = hashtagService.getHashtagIdByHashtagName(hashtagName);
        List<Board> beforeBoardList = boardService.getBoardListByHashtagId(hashtagId);

        /* Board 객체에 Hashtag 리스트를 찾아서 add 해주고 반환한다. */
        return beforeBoardList.stream().peek(board -> {
            board.addHashtagList(boardHashtagService.getHashtagNameByBoardId(board.getBoardId()));
        }).toList();
    }

    public Boolean isWriter(String boardId, Long userId) {
        return boardService.isWriter(boardId, userId);
    }

    @Transactional
    public Board updateBoardAndHashtag(UpdateBoardCommand command) {
        BoardEntity boardEntity = boardService.getBoardEntityByBoardId(command.getBoardId());

        /* 해시태그에 수정 사항이 있으면 해당 게시글의 해시태그 모두 삭제 후 요청 값으로 insert */
        if(!command.getHashtagList().isEmpty()) {
            boardHashtagService.deleteHashtag(command.getBoardId());
            List<HashtagEntity> hashtagEntityList = hashtagService.makeHashtagList(command.getHashtagList());
            List<BoardHashtagEntity> boardHashtagEntityList = boardHashtagService.makeBoardHashtagList(boardEntity, hashtagEntityList);

            return boardService.updateBoardWithHashtag(command, boardEntity, boardHashtagEntityList);
        }
        return boardService.updateBoard(command, boardEntity);
    }

    public void delete(String boardId) {
        boardService.delete(boardId);
    }
}

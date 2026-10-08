package com.tagnote.core.domain.boardusertag.service;

import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import com.tagnote.core.domain.usertag.UserTagEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardUserTagService {

    private final BoardUserTagRepository boardUserTagRepository;

    /**
     *  중간 테이블 저장을 위하여 BoardUserTag의 값을 만든다.
     */
    public List<BoardUserTagEntity> makeBoardUserTagList(BoardEntity boardEntity, List<UserTagEntity> userTagEntityList) {
        return userTagEntityList
                .stream()
                .map(userTagEntity ->
                        BoardUserTagEntity.of(boardEntity, userTagEntity))
                .toList();
    }

    /**
     * 게시글 아이디로 유저 태그 이름을 찾는다.
     */
    public UserTagNames getUserTagNameByBoardId(String boardId) {
        List<String> nameList = boardUserTagRepository.findUserTagNameByBoardId(boardId);
        return UserTagNames.ofNameList(nameList);
    }

    /**
     * 유저 태그를 삭제한다.
     */
    @Transactional
    public void deleteUserTag(String boardId) {
        boardUserTagRepository.deleteByBoard_BoardId(boardId);
    }

    /**
     * 최근 작성된 게시글 5개의 BoardUserTag 객체를 조회한다.
     */
    public List<BoardUserTag> getRecentBoardUserTagList() {
        List<BoardUserTagEntity> boardUserTagEntityList = boardUserTagRepository.findRecentBoardUserTagList(PageRequest.of(0, 5));
        return boardUserTagEntityList
                .stream()
                .map(BoardUserTagEntity::toBoardUserTag)
                .toList();
    }
}

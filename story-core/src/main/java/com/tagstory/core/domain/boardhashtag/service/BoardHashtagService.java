package com.tagstory.core.domain.boardhashtag.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.boardhashtag.repository.BoardHashtagRepository;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNames;
import com.tagstory.core.domain.hashtag.HashtagEntity;
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
public class BoardHashtagService {

    private final BoardHashtagRepository boardHashtagRepository;

    /**
     *  중간 테이블 저장을 위하여 BoardHashtag의 값을 만든다.
     */
    public List<BoardHashtagEntity> makeBoardHashtagList(BoardEntity boardEntity, List<HashtagEntity> hashtagEntityList) {
        return hashtagEntityList
                .stream()
                .map(hashtagEntity ->
                        BoardHashtagEntity.of(boardEntity, hashtagEntity))
                .toList();
    }

    /**
     * 게시글 아이디로 해시태그 이름을 찾는다.
     */
    public HashtagNames getHashtagNameByBoardId(String boardId) {
        List<String> nameList = boardHashtagRepository.findHashtagNameByBoardId(boardId);
        return HashtagNames.ofNameList(nameList);
    }

    /**
     * 해시태그를 삭제한다.
     */
    @Transactional
    public void deleteHashtag(String boardId) {
        boardHashtagRepository.deleteByBoard_BoardId(boardId);
    }

    /**
     * 최근 작성된 게시글 5개의 BoardHashtag 객체를 조회한다.
     */
    public List<BoardHashtag> getRecentBoardHashtagList() {
        List<BoardHashtagEntity> boardHashtagEntityList = boardHashtagRepository.findRecentBoardHashtagList(PageRequest.of(0, 5));
        return boardHashtagEntityList
                .stream()
                .map(BoardHashtagEntity::toBoardHashtag)
                .toList();
    }
}

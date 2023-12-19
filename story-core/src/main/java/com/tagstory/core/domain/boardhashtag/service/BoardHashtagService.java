package com.tagstory.core.domain.boardhashtag.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import com.tagstory.core.domain.boardhashtag.repository.BoardHashtagRepository;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardHashtagService {

    private final BoardHashtagRepository boardHashtagRepository;

    public List<BoardHashtagEntity> makeBoardHashtagList(BoardEntity boardEntity, List<HashtagEntity> hashtagEntityList) {
        return hashtagEntityList.stream().map(hashtagEntity -> BoardHashtagEntity.of(boardEntity, hashtagEntity)).collect(Collectors.toList());
    }

    public HashtagNameList getHashtagNameByBoardId(String boardId) {
        List<String> nameList = boardHashtagRepository.findHashtagNameByBoardId(boardId);
        return HashtagNameList.onComplete(nameList);
    }

    @Transactional
    public void deleteHashtag(String boardId) {
        boardHashtagRepository.deleteByBoard_BoardId(boardId);
    }
}

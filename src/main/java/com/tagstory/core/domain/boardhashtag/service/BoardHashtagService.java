package com.tagstory.core.domain.boardhashtag.service;

import com.tagstory.core.domain.boardhashtag.repository.BoardHashtagRepository;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardHashtagService {

    private final BoardHashtagRepository boardHashtagRepository;

    public HashtagNameList getHashtagName(String boardId) {
        List<String> nameList = boardHashtagRepository.findHashtagNameByBoardId(boardId);
        return HashtagNameList.onComplete(nameList);
    }
}

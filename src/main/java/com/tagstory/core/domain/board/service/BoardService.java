package com.tagstory.core.domain.board.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.board.dto.command.CreateBoardCommand;
import com.tagstory.core.domain.board.dto.response.BoardByTrackResponse;
import com.tagstory.core.domain.board.dto.response.CreateBoardResponse;
import com.tagstory.core.domain.board.repository.BoardRepository;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import com.tagstory.core.domain.user.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;

    @Transactional
    public CreateBoardResponse create(CreateBoardCommand createBoardCommand, UserEntity userEntity, List<HashtagEntity> hashtagEntityList) {
        BoardEntity beforeBoardEntity = BoardEntity.create(createBoardCommand);
        beforeBoardEntity.addUser(userEntity);
        beforeBoardEntity.addHashtag(hashtagEntityList);
        BoardEntity savedBoardEntity = boardRepository.save(beforeBoardEntity);
        return CreateBoardResponse.onComplete(savedBoardEntity);
    }

    public List<BoardByTrackResponse> getBoardListByTrackId(String trackId) {
        List<BoardEntity> boardEntityList = boardRepository.findByStatusAndTrackIdOrderByBoardIdDesc(BoardStatus.POST, trackId);
        return boardEntityList.stream()
                .map(BoardByTrackResponse::onComplete)
                .collect(Collectors.toList());
    }

    public BoardEntity findByBoardId(Long boardId) {
        return boardRepository.getReferenceById(boardId);
    }
}

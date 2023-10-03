package com.tagstory.core.domain.like.service;

import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.like.LikeEntity;
import com.tagstory.core.domain.like.repository.LikeRepository;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;

    @Transactional
    public void like(Board board, User user) {
        LikeEntity like = LikeEntity.createLike(user, board);
        likeRepository.save(like);
    }

    @Transactional
    public void cancelLike(Board board, User user) {
        likeRepository.deleteByBoardAndUser(board.toEntity(), user.toEntity());
    }

    public int getLikeCount(Board board) {
        return likeRepository.countByBoard(board.toEntity());
    }

    public boolean isLiked(String boardId, Long userId) {
        LikeEntity likeEntity = likeRepository.findByBoard_BoardId_AndUser_UserId(boardId, userId);
        return Objects.nonNull(likeEntity);
    }
}

package com.tagstory.core.domain.like.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.like.LikeEntity;
import com.tagstory.core.domain.like.dto.response.LikeCount;
import com.tagstory.core.domain.like.repository.LikeRepository;
import com.tagstory.core.domain.user.repository.dto.CacheUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {
    private final LikeRepository likeRepository;

    @Transactional
    public void like(BoardEntity board, CacheUser user) {
        LikeEntity like = LikeEntity.createLike(board, user);
        likeRepository.save(like);
    }

    @Transactional
    public void cancelLike(BoardEntity board, CacheUser user) {
        likeRepository.deleteByBoardAndUser(board, CacheUser.toEntity(user));
    }

    public LikeCount getLikeCount(BoardEntity board) {
        int likeCount = likeRepository.countByBoard(board);
        return LikeCount.onComplete(likeCount);
    }
}

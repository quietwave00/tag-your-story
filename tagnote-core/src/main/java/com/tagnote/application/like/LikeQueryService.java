package com.tagnote.application.like;

import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.infrastructure.persistence.like.LikeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeQueryService {
    private final LikeJpaRepository likes;

    public boolean isLiked(String boardId, Long userId) {
        return likes.existsByBoard_BoardIdAndUser_UserId(boardId, userId);
    }

    public LikedBoardPage likedBoards(Long userId, int page) {
        Page<LikedBoardItem> result = likes.findLikedBoards(userId, BoardStatus.POST, PageRequest.of(page, 8))
                .map(like -> new LikedBoardItem(like.getBoard().getBoardId(),
                        like.getBoard().getUserEntity().getDisplayNickname(), like.getBoard().getContent()));
        return new LikedBoardPage(result.getContent(), result.getTotalElements());
    }
}

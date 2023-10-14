package com.tagstory.core.domain.comment.repository;

import com.tagstory.core.domain.comment.CommentEntity;
import com.tagstory.core.domain.comment.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    List<CommentEntity> findByBoardEntity_BoardId(String boardId);


    List<CommentEntity> findByBoardEntity_BoardIdAndUserEntity_UserId(String boardId, Long userId);

    List<CommentEntity> findByStatusAndBoardEntity_BoardIdOrderByCommentIdDesc(CommentStatus post, String boardId);
}

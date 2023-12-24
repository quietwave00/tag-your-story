package com.tagstory.core.domain.comment.repository;

import com.tagstory.core.domain.comment.CommentEntity;
import com.tagstory.core.domain.comment.CommentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    List<CommentEntity> findByBoardEntity_BoardId(String boardId);


    List<CommentEntity> findByBoardEntity_BoardIdAndUserEntity_UserId(String boardId, Long userId);

    Optional<Page<CommentEntity>> findByStatusAndParentIsNullAndBoardEntity_BoardIdOrderByCommentIdDesc(CommentStatus commentStatus, String boardId, PageRequest pageRequest);

    int countByBoardEntity_BoardIdAndStatus(String boardId, CommentStatus commentStatus);

    Page<CommentEntity> findByParentAndStatus(com.tagstory.core.domain.comment.CommentEntity entity, CommentStatus commentStatus, PageRequest of);
}

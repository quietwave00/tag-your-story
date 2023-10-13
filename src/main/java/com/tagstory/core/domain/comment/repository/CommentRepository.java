package com.tagstory.core.domain.comment.repository;

import com.tagstory.core.domain.comment.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

}

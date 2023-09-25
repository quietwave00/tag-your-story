package com.tagstory.core.domain.boardhashtag.repository;

import com.tagstory.core.domain.boardhashtag.BoardHashtagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardHashtagRepository extends JpaRepository<BoardHashtagEntity, Long> {
}

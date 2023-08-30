package com.tagstory.core.domain.board.repository;

import com.tagstory.core.domain.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {

}

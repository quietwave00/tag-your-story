package com.tagstory.core.domain.file.repository;

import com.tagstory.core.domain.file.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long>, CacheFileRepository {
    List<FileEntity> findByBoard_BoardId(String boardId);
}

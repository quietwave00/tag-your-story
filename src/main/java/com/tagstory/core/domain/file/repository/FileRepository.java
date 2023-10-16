package com.tagstory.core.domain.file.repository;

import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.file.FileLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long>, CacheFileRepository {
    List<FileEntity> findByBoard_BoardId(String boardId);

    List<FileEntity> findByFileLevelAndBoard_BoardIdIn(FileLevel fileLevel, List<String> boardIdList);
}

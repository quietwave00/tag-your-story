package com.tagstory.core.domain.file.repository;

import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.file.FileLevel;
import com.tagstory.core.domain.file.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long>, CacheFileRepository {
    List<FileEntity> findByStatusAndBoard_BoardId(FileStatus status, String boardId);

    List<FileEntity> findByFileLevelAndStatusAndBoard_BoardIdIn(FileLevel fileLevel, FileStatus status, List<String> boardIdList);
    List<FileEntity> findByFileIdIn(List<Long> fileIdList);
}

package com.tagstory.core.domain.file.repository;

import com.tagstory.core.domain.file.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<FileEntity, Long>, CacheFileRepository {
}

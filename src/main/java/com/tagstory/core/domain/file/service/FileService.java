package com.tagstory.core.domain.file.service;

import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.file.FileLevel;
import com.tagstory.core.domain.file.dto.S3File;
import com.tagstory.core.domain.file.dto.response.FileList;
import com.tagstory.core.domain.file.dto.response.MainFile;
import com.tagstory.core.domain.file.dto.response.UploadFile;
import com.tagstory.core.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;

    @Transactional
    public List<UploadFile> upload(List<S3File> saveFileList, BoardEntity board) {
        List<FileEntity> fileList = saveFileList.stream()
                .map(beforeS3File -> {
                    S3File s3File = addFileLevel(beforeS3File);
                    return s3File.toEntity(board);
                })
                .collect(Collectors.toList());
        fileRepository.saveCache(fileList, CacheSpec.FILE);
        return fileRepository.saveAll(fileList).stream()
                .map(UploadFile::onComplete)
                .collect(Collectors.toList());
    }

    public List<MainFile> getMainFileList(List<BoardEntity> boardList) {
        return boardList.stream()
                .flatMap(board -> board.getFileList().stream())
                .filter(file -> file.getFileLevel() == FileLevel.MAIN)
                .map(MainFile::onComplete)
                .collect(Collectors.toList());
    }

    public List<FileList> getFileList(BoardEntity board) {
        List<FileEntity> fileList = board.getFileList();
        return fileList.stream()
                .map(FileList::onComplete)
                .collect(Collectors.toList());
    }

    private S3File addFileLevel(S3File s3File) {
        FileLevel fileLevel = (s3File.getIndex() == 0) ? FileLevel.MAIN : FileLevel.SUB;
        return s3File.addFileLevel(fileLevel);
    }
}

package com.tagstory.core.domain.file.service;

import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.file.FileLevel;
import com.tagstory.core.domain.file.dto.S3File;
import com.tagstory.core.domain.file.dto.response.File;
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
    public List<File> upload(List<S3File> saveFileList, Board board) {
        /* S3에 저장돈 정보를 엔티티로 변환 */
        List<FileEntity> fileEntityList = saveFileList.stream()
                .map(beforeS3File -> {
                    S3File s3File = addFileLevel(beforeS3File);
                    return s3File.toEntity();
                })
                .collect(Collectors.toList());

        fileRepository.saveCache(fileEntityList, CacheSpec.FILE);
        return fileRepository.saveAll(fileEntityList).stream()
                .map(FileEntity::toFile)
                .collect(Collectors.toList());
    }

    public List<File> getMainFileList(List<Board> boardList) {
        return boardList.stream()
                .flatMap(board -> board.getFileList().stream())
                .filter(file -> file.getFileLevel() == FileLevel.MAIN)
                .collect(Collectors.toList());
    }

    public List<File> getFileList(Board board) {
        return board.getFileList();
    }

    /*
     * 단일 메소드
     */
    private S3File addFileLevel(S3File s3File) {
        FileLevel fileLevel = (s3File.getIndex() == 0) ? FileLevel.MAIN : FileLevel.SUB;
        return s3File.addFileLevel(fileLevel);
    }
}

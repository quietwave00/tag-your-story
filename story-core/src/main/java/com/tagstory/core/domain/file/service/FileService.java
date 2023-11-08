package com.tagstory.core.domain.file.service;

import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.file.FileLevel;
import com.tagstory.core.domain.file.FileStatus;
import com.tagstory.core.domain.file.dto.S3File;
import com.tagstory.core.domain.file.dto.command.DeleteFileCommand;
import com.tagstory.core.domain.file.dto.response.File;
import com.tagstory.core.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;

    @Transactional
    public List<File> upload(List<S3File> saveFileList, Board board) {
        /* S3에 저장된 정보를 엔티티로 변환 */
        List<FileEntity> fileEntityList = saveFileList.stream()
                .map(beforeS3File -> {
                    S3File s3File = addFileLevel(beforeS3File);
                    return s3File.toEntity();
                })
                .collect(Collectors.toList());

        /* 변환한 엔티티 객체를 저장 */
        fileEntityList.forEach(fileEntity -> fileEntity.addBoard(board.toEntity()));
        List<FileEntity> savedFileEntityList= new ArrayList<>(fileRepository.saveAll(fileEntityList));

        /* 캐싱 */
        fileRepository.saveCache(savedFileEntityList, CacheSpec.FILE);
        return savedFileEntityList.stream().map(FileEntity::toFile).collect(Collectors.toList());
    }

    public List<File> getMainFileList(List<Board> boardList) {
        List<String> boardIdList = boardList.stream().map(Board::getBoardId).collect(Collectors.toList());
        return getMainFileListByBoardId(boardIdList);
    }

    public List<File> getFileList(String boardId) {
        return getFileListByBoardId(boardId);
    }

    @Transactional
    public void deleteFile(DeleteFileCommand command) {
        getFileEntityListByFileId(command.getFileIdList()).forEach(FileEntity::delete);
        fileRepository.saveFileIdsToDelete(command.getFileIdList(), CacheSpec.FILE_TO_DELETE);
    }

    /*
     * 단일 메소드
     */
    public List<FileEntity> getFileEntityListByFileId(List<Long> fileIdList) {
        return fileRepository.findByFileIdIn(fileIdList);
    }

    private S3File addFileLevel(S3File s3File) {
        FileLevel fileLevel = (s3File.getIndex() == 0) ? FileLevel.MAIN : FileLevel.SUB;
        return s3File.addFileLevel(fileLevel);
    }

    private List<File> getFileListByBoardId(String boardId) {
        return fileRepository.findByStatusAndBoard_BoardId(FileStatus.POST, boardId).stream().map(FileEntity::toFile).collect(Collectors.toList());
    }

    private List<File> getMainFileListByBoardId(List<String> boardIdList) {
        return fileRepository.findByFileLevelAndStatusAndBoard_BoardIdIn(FileLevel.MAIN, FileStatus.POST, boardIdList)
                .stream().map(FileEntity::toFile).collect(Collectors.toList());
    }
}

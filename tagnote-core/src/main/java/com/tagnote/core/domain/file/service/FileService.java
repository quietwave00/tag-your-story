package com.tagnote.core.domain.file.service;

import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.file.FileEntity;
import com.tagnote.core.domain.file.FileLevel;
import com.tagnote.core.domain.file.FileStatus;
import com.tagnote.core.domain.file.dto.StoredFile;
import com.tagnote.core.domain.file.dto.command.DeleteFileCommand;
import com.tagnote.core.domain.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileRepository fileRepository;

    @Transactional
    public List<File> upload(List<StoredFile> savedFileList, Board board) {
        /* 파일 스토리지에 저장된 정보를 엔티티로 변환 */
        List<FileEntity> fileEntityList = savedFileList.stream()
                .map(beforeStoredFile -> {
                    StoredFile storedFile = addFileLevel(beforeStoredFile);
                    return storedFile.toEntity();
                })
                .toList();

        /* 변환한 엔티티 객체를 저장 */
        fileEntityList.forEach(fileEntity -> fileEntity.addBoard(board.toEntity()));
        List<FileEntity> savedFileEntityList= new ArrayList<>(fileRepository.saveAll(fileEntityList));

        /* 캐싱 */
        fileRepository.saveCache(savedFileEntityList, CacheSpec.FILE);
        return savedFileEntityList.stream().map(FileEntity::toFile).toList();
    }

    @Transactional
    public List<File> update(List<StoredFile> savedFileList, Board board) {
        List<FileEntity> fileEntityList = savedFileList.stream()
                .map(storedFile -> {
                    return storedFile.setFileLevelToSub().toEntity();
                })
                .toList();

        fileEntityList.forEach(fileEntity -> fileEntity.addBoard(board.toEntity()));
        List<FileEntity> savedFileEntityList= new ArrayList<>(fileRepository.saveAll(fileEntityList));
        return savedFileEntityList.stream().map(FileEntity::toFile).toList();
    }

    public List<File> getMainFileList(List<Board> boardList) {
        List<String> boardIdList = boardList.stream().map(Board::getBoardId).toList();
        return getMainFileListByBoardId(boardIdList);
    }

    public List<File> getFileList(String boardId) {
        return getFileListByBoardId(boardId);
    }

    @Transactional
    public void deleteFile(DeleteFileCommand command) {
        /* status 변경 */
        getFileEntityListByFileId(command.getFileIdList()).forEach(FileEntity::delete);

        /* 파일이 삭제됨에 따라 FileLevel을 재설정한다. */
        setFileLevel(command.getBoardId());
    }

    /*
     * 단일 메소드
     */
    public List<FileEntity> getFileEntityListByFileId(List<Long> fileIdList) {
        return fileRepository.findByFileIdIn(fileIdList);
    }

    private StoredFile addFileLevel(StoredFile storedFile) {
        FileLevel fileLevel = (storedFile.getIndex() == 0) ? FileLevel.MAIN : FileLevel.SUB;
        return storedFile.addFileLevel(fileLevel);
    }

    private List<File> getFileListByBoardId(String boardId) {
        return fileRepository.findByStatusAndBoard_BoardIdOrderByFileId(FileStatus.POST, boardId).stream().map(FileEntity::toFile).toList();
    }

    private List<FileEntity> getFileEntityListByBoardId(String boardId) {
        return fileRepository.findByStatusAndBoard_BoardIdOrderByFileId(FileStatus.POST, boardId);
    }

    private List<File> getMainFileListByBoardId(List<String> boardIdList) {
        return fileRepository.findByFileLevelAndStatusAndBoard_BoardIdIn(FileLevel.MAIN, FileStatus.POST, boardIdList)
                .stream().map(FileEntity::toFile)
                .toList();
    }

    private void setFileLevel(String boardId) {
        /* 메인 설정 */
        List<FileEntity> fileEntityList = getFileEntityListByBoardId(boardId);
        if(!fileEntityList.isEmpty()) {
            FileEntity mainFileEntity = fileEntityList.get(0);
            mainFileEntity.setFileLevelToMain();

            /* 서브 설정 */
            List<Long> subFileIdList = fileEntityList.subList(1, fileEntityList.size())
                    .stream()
                    .map(FileEntity::getFileId)
                    .toList();

            fileRepository.updateFileLevel(subFileIdList, FileLevel.SUB);
        }
    }
}

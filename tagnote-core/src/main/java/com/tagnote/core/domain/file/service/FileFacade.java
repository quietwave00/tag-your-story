package com.tagnote.core.domain.file.service;

import com.tagnote.core.domain.board.service.Board;
import com.tagnote.core.domain.board.service.BoardService;
import com.tagnote.core.domain.file.dto.StoredFile;
import com.tagnote.core.domain.file.dto.command.DeleteFileCommand;
import com.tagnote.core.domain.file.dto.command.UploadFileCommand;
import com.tagnote.core.domain.file.storage.FileStorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FileFacade {
    private final FileService fileService;
    private final BoardService boardService;
    private final FileStorageClient fileStorageClient;

    public List<File> upload(List<MultipartFile> fileList, UploadFileCommand command) {
        List<StoredFile> savedFileList = fileStorageClient.uploadFiles(fileList);
        Board board = boardService.getBoardByBoardId(command.getBoardId());
        return fileService.upload(savedFileList, board);
    }

    public List<File> update(List<MultipartFile> fileList, UploadFileCommand command) {
        List<StoredFile> savedFileList = fileStorageClient.uploadFiles(fileList);
        Board board = boardService.getBoardByBoardId(command.getBoardId());
        return fileService.update(savedFileList, board);
    }

    public List<File> getMainFileList(String trackId, int page) {
        List<Board> boardList = boardService.findByTrackId(trackId, page);
        return fileService.getMainFileList(boardList);
    }

    public List<File> getFileList(String boardId) {
        return fileService.getFileList(boardId);
    }

    public void deleteFile(DeleteFileCommand command) {
        fileService.deleteFile(command);
    }
}

package com.tagstory.core.domain.file.service;

import com.tagstory.core.domain.board.dto.response.Board;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.file.dto.S3File;
import com.tagstory.core.domain.file.dto.command.UploadFileCommand;
import com.tagstory.core.domain.file.dto.response.File;
import com.tagstory.core.domain.file.webclient.S3WebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FileFacade {
    private final FileService fileService;
    private final BoardService boardService;
    private final S3WebClient s3WebClient;

    public List<File> upload(List<MultipartFile> fileList, UploadFileCommand uploadFileCommand) {
        List<S3File> savedFileList = s3WebClient.uploadFiles(fileList);
        Board board = boardService.getBoardByBoardId(uploadFileCommand.getBoardId());
        return fileService.upload(savedFileList, board);
    }

    public List<File> getMainFileList(String trackId) {
        List<Board> boardList = boardService.findByTrackId(trackId);
        return fileService.getMainFileList(boardList);
    }

    public List<File> getFileList(String boardId) {
        Board board = boardService.getBoardByBoardId(boardId);
        return fileService.getFileList(board);
    }
}

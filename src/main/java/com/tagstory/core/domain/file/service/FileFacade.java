package com.tagstory.core.domain.file.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.service.BoardService;
import com.tagstory.core.domain.file.dto.S3File;
import com.tagstory.core.domain.file.dto.command.UploadFileCommand;
import com.tagstory.core.domain.file.dto.response.MainFile;
import com.tagstory.core.domain.file.dto.response.UploadFile;
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

    public List<UploadFile> upload(List<MultipartFile> fileList, UploadFileCommand uploadFileCommand) {
        List<S3File> saveFileList = s3WebClient.uploadFiles(fileList);
        BoardEntity board = boardService.findByBoardId(uploadFileCommand.getBoardId());
        return fileService.upload(saveFileList, board);
    }

    public List<MainFile> getMainFileList(String trackId) {
        List<BoardEntity> boardList = boardService.findByTrackId(trackId);
        return fileService.getMainFileList(boardList);
    }
}

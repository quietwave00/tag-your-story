package com.tagstory.core.domain.board.dto.response;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.file.FileEntity;
import com.tagstory.core.domain.hashtag.HashtagEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class DetailBoard {
    private String content;
    private String nickname;
    private LocalDateTime createdAt;
    private List<String> hashtagList;
    private List<String> filePathList;

    public static DetailBoard onComplete(BoardEntity board) {
        return DetailBoard.builder()
                .content(board.getContent())
                .nickname(board.getUser().getNickname())
                .createdAt(board.getCreatedAt())
                .hashtagList(board.getHashtagList().stream()
                        .map(HashtagEntity::getName)
                        .collect(Collectors.toList()))
                .filePathList(board.getFileList().stream()
                        .map(FileEntity::getFilePath)
                        .collect(Collectors.toList()))
                .build();
    }
}

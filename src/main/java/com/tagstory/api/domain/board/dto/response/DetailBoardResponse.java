package com.tagstory.api.domain.board.dto.response;

import com.tagstory.core.domain.board.dto.response.DetailBoard;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DetailBoardResponse {
    private String content;
    private String nickname;
    private LocalDateTime createdAt;
    private List<String> hashtagList;
    private List<String> filePathList;

    public static DetailBoardResponse create(DetailBoard detailBoard) {
        return DetailBoardResponse.builder()
                .content(detailBoard.getContent())
                .nickname(detailBoard.getNickname())
                .createdAt(detailBoard.getCreatedAt())
                .hashtagList(detailBoard.getHashtagList())
                .filePathList(detailBoard.getFilePathList())
                .build();
    }
}

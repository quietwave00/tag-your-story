package com.tagstory.core.domain.board.service;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.board.BoardStatus;
import com.tagstory.core.domain.boardhashtag.service.dto.HashtagNameList;
import com.tagstory.core.domain.file.service.File;
import com.tagstory.core.domain.user.service.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Board {
    private String boardId;

    private String content;

    private BoardStatus status;

    private Integer count;

    private Integer likeCount;

    private String trackId;

    private User user;

    private HashtagNameList hashtagNameList;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<File> fileList;

    /*
     * 형변환
     */
    public BoardEntity toEntity() {
        return BoardEntity.builder()
                .boardId(this.getBoardId())
                .content(this.getContent())
                .status(this.getStatus())
                .count(this.getCount())
                .likeCount(this.getLikeCount())
                .trackId(this.getTrackId())
                .userEntity(this.getUser().toEntity())
                .build();
    }

    /*
     * 비즈니스 로직
     */
    public Board addHashtagList(HashtagNameList hashtagNameList) {
        this.hashtagNameList = hashtagNameList;
        return this;
    }
}

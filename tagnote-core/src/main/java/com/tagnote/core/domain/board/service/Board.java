package com.tagnote.core.domain.board.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.boardusertag.service.BoardUserTag;
import com.tagnote.core.domain.boardusertag.service.dto.UserTagNames;
import com.tagnote.core.domain.file.service.File;
import com.tagnote.core.domain.user.service.User;
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

    private UserTagNames userTagNameList;

    @JsonIgnore
    private List<BoardUserTag> boardUserTagList;

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
    public Board addUserTagList(UserTagNames userTagNameList) {
        this.userTagNameList = userTagNameList;
        return this;
    }
}

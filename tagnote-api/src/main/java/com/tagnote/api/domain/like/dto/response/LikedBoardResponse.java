package com.tagnote.api.domain.like.dto.response;

import com.tagnote.application.like.LikedBoardPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record LikedBoardResponse(
        @Schema(description = "좋아요한 공개 게시글 목록. page size 8") List<Item> items,
        @Schema(description = "전체 공개 게시글 수") long totalCount) {

    public static LikedBoardResponse from(LikedBoardPage page) {
        return new LikedBoardResponse(page.items().stream()
                .map(item -> new Item(item.boardId(), item.author(), item.content())).toList(),
                page.totalCount());
    }

    public record Item(
            @Schema(description = "게시글 ID. 기존 Board 상세 API의 boardId로 사용") String boardId,
            @Schema(description = "작성자 표시 이름. 탈퇴 계정은 탈퇴한 사용자") String author,
            @Schema(description = "게시글 본문") String content) {
    }
}

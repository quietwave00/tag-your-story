package com.tagnote.api.domain.board.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBoardRequest {
    @NotNull(message = "content는 비어 있을 수 없습니다.")
    @Schema(description = "Board 본문", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "Spotify Track ID. albumId와 동시에 전달할 수 없다.", nullable = true)
    private String trackId;

    @Schema(description = "Spotify Album ID. trackId와 동시에 전달할 수 없다.", nullable = true)
    private String albumId;

    @NotEmpty(message = "유저 태그는 비어 있을 수 없습니다.")
    @Schema(description = "작성자 소유 UserTag 이름. 최소 1개", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> userTagList;

    public CreateBoardCommand toCommand(Long userId) {
        return CreateBoardCommand.builder()
                .content(content)
                .trackId(trackId)
                .albumId(albumId)
                .userTagList(userTagList)
                .userId(userId)
                .build();
    }

    @JsonIgnore
    @AssertTrue(message = "trackId와 albumId 중 하나만 입력해야 합니다.")
    public boolean isSubjectValid() {
        boolean hasTrack = trackId != null && !trackId.isBlank();
        boolean hasAlbum = albumId != null && !albumId.isBlank();
        return hasTrack != hasAlbum;
    }
}

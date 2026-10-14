package com.tagnote.application.board;

import com.tagnote.application.board.model.BoardSubject;
import com.tagnote.application.board.model.BoardView;
import com.tagnote.application.catalog.detail.model.AlbumDetail;
import com.tagnote.application.catalog.detail.model.TrackDetail;
import com.tagnote.application.catalog.selection.AlbumSelectionService;
import com.tagnote.application.catalog.selection.TrackSelectionService;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import com.tagnote.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BoardCommandService {

    private final TrackSelectionService trackSelectionService;
    private final AlbumSelectionService albumSelectionService;
    private final BoardTransactionalWriter writer;
    private final BoardQueryService queryService;

    public BoardView create(CreateBoardCommand command) {
        BoardSubject subject = selectSubject(command);
        String boardId = writer.create(command, subject);
        return queryService.getDetail(boardId);
    }

    public BoardView update(UpdateBoardCommand command, Long currentUserId) {
        String boardId = writer.update(command, currentUserId);
        return queryService.getDetail(boardId);
    }

    public void delete(String boardId, Long currentUserId) {
        writer.delete(boardId, currentUserId);
    }

    private BoardSubject selectSubject(CreateBoardCommand command) {
        boolean hasTrack = StringUtils.hasText(command.getTrackId());
        boolean hasAlbum = StringUtils.hasText(command.getAlbumId());
        if (hasTrack == hasAlbum) {
            throw new CustomException(ExceptionCode.INVALID_BOARD_SUBJECT);
        }
        if (hasTrack) {
            TrackDetail detail = trackSelectionService.select(command.getTrackId());
            return BoardSubject.track(
                    detail.track().getCatalogTrackId(), detail.track().getSpotifyTrackId()
            );
        }
        AlbumDetail detail = albumSelectionService.select(command.getAlbumId());
        return BoardSubject.album(detail.album().getAlbumId(), detail.album().getSpotifyAlbumId());
    }
}

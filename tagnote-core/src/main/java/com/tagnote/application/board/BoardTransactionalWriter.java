package com.tagnote.application.board;

import com.tagnote.application.user.ActiveUserWriteGuard;
import com.tagnote.application.board.model.BoardSubject;
import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.board.dto.command.CreateBoardCommand;
import com.tagnote.core.domain.board.dto.command.UpdateBoardCommand;
import com.tagnote.core.domain.boardusertag.BoardUserTagEntity;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.usertag.UserTagEntity;
import com.tagnote.core.domain.usertag.service.UserTagService;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.infrastructure.persistence.board.BoardJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.AlbumJpaRepository;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BoardTransactionalWriter {

    private final BoardJpaRepository boardRepository;
    private final BoardUserTagRepository boardUserTagRepository;
    private final ActiveUserWriteGuard activeUserWriteGuard;
    private final TrackJpaRepository trackRepository;
    private final AlbumJpaRepository albumRepository;
    private final UserTagService userTagService;

    @Transactional
    public String create(CreateBoardCommand command, BoardSubject subject) {
        UserEntity writer = activeUserWriteGuard.lock(command.getUserId());
        BoardEntity board = createBoard(command.getContent(), writer, subject);
        BoardEntity saved = boardRepository.saveAndFlush(board);

        List<UserTagEntity> userTags = userTagService.makeUserTagList(writer, command.getUserTagList());
        List<BoardUserTagEntity> joins = userTags.stream()
                .map(userTag -> BoardUserTagEntity.of(saved, userTag))
                .toList();
        boardUserTagRepository.saveAll(joins);
        return saved.getBoardId();
    }

    @Transactional
    public String update(UpdateBoardCommand command, Long currentUserId) {
        activeUserWriteGuard.lock(currentUserId);
        BoardEntity board = getPublished(command.getBoardId());
        requireWriter(board, currentUserId);
        board.update(command.getContent());

        if (command.getUserTagList() != null && !command.getUserTagList().isEmpty()) {
            List<UserTagEntity> userTags = userTagService.makeUserTagList(
                    board.getUserEntity(), command.getUserTagList()
            );
            boardUserTagRepository.deleteByBoard_BoardId(board.getBoardId());
            boardUserTagRepository.saveAll(userTags.stream()
                    .map(userTag -> BoardUserTagEntity.of(board, userTag))
                    .toList());
        }
        return board.getBoardId();
    }

    @Transactional
    public void delete(String boardId, Long currentUserId) {
        activeUserWriteGuard.lock(currentUserId);
        BoardEntity board = getPublished(boardId);
        requireWriter(board, currentUserId);
        board.delete();
    }

    private BoardEntity createBoard(String content, UserEntity writer, BoardSubject subject) {
        if (subject.type() == SubjectType.TRACK) {
            TrackEntity track = trackRepository.findById(subject.catalogId())
                    .orElseThrow(() -> new CustomException(ExceptionCode.TRACK_NOT_FOUND));
            return BoardEntity.createForTrack(content, writer, track);
        }
        AlbumEntity album = albumRepository.findById(subject.catalogId())
                .orElseThrow(() -> new CustomException(ExceptionCode.ALBUM_NOT_FOUND));
        return BoardEntity.createForAlbum(content, writer, album);
    }

    private BoardEntity getPublished(String boardId) {
        return boardRepository.findByBoardIdAndStatus(boardId, BoardStatus.POST)
                .orElseThrow(() -> new CustomException(ExceptionCode.BOARD_NOT_FOUND));
    }

    private void requireWriter(BoardEntity board, Long currentUserId) {
        if (!Objects.equals(board.getUserEntity().getUserId(), currentUserId)) {
            throw new CustomException(ExceptionCode.BOARD_ACCESS_DENIED);
        }
    }
}

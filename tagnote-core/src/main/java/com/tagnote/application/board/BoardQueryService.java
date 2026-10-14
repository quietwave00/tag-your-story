package com.tagnote.application.board;

import com.tagnote.application.board.model.BoardPage;
import com.tagnote.application.board.model.BoardSubject;
import com.tagnote.application.board.model.BoardSystemTag;
import com.tagnote.application.board.model.BoardView;
import com.tagnote.core.domain.board.BoardEntity;
import com.tagnote.core.domain.board.BoardOrderType;
import com.tagnote.core.domain.board.BoardStatus;
import com.tagnote.core.domain.boardusertag.repository.BoardUserTagRepository;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.domain.enrichment.subject.SubjectRef;
import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.resolution.ResolvedStatus;
import com.tagnote.domain.resolution.SubjectTagResolvedEntity;
import com.tagnote.infrastructure.persistence.board.BoardJpaRepository;
import com.tagnote.infrastructure.persistence.board.BoardUserTagNameRow;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardQueryService {

    private static final int PAGE_SIZE = 8;

    private final BoardJpaRepository boardRepository;
    private final BoardUserTagRepository boardUserTagRepository;
    private final SubjectTagResolvedJpaRepository resolvedRepository;

    public BoardPage getTrackBoards(String spotifyTrackId, BoardOrderType orderType, int page) {
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE);
        Page<BoardEntity> boards = orderType.isCreatedAt()
                ? boardRepository.findByStatusAndTrack_SpotifyIdOrderByCreatedAtDesc(
                        BoardStatus.POST, spotifyTrackId, pageable)
                : boardRepository.findByStatusAndTrack_SpotifyIdOrderByLikeCountDesc(
                        BoardStatus.POST, spotifyTrackId, pageable);
        return toPage(boards);
    }

    public BoardPage getAlbumBoards(String spotifyAlbumId, BoardOrderType orderType, int page) {
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE);
        Page<BoardEntity> boards = orderType.isCreatedAt()
                ? boardRepository.findByStatusAndAlbum_SpotifyIdOrderByCreatedAtDesc(
                        BoardStatus.POST, spotifyAlbumId, pageable)
                : boardRepository.findByStatusAndAlbum_SpotifyIdOrderByLikeCountDesc(
                        BoardStatus.POST, spotifyAlbumId, pageable);
        return toPage(boards);
    }

    public BoardView getDetail(String boardId) {
        BoardEntity board = getPublished(boardId);
        return assemble(List.of(board)).get(0);
    }

    public int countTrackBoards(String spotifyTrackId) {
        return boardRepository.countByTrack_SpotifyIdAndStatus(spotifyTrackId, BoardStatus.POST);
    }

    public int countAlbumBoards(String spotifyAlbumId) {
        return boardRepository.countByAlbum_SpotifyIdAndStatus(spotifyAlbumId, BoardStatus.POST);
    }

    public List<BoardView> getBoardsByUserTagName(String name) {
        List<BoardEntity> boards = boardRepository.findBoardsByUserTagName(name, BoardStatus.POST);
        if (boards.isEmpty()) {
            throw new CustomException(ExceptionCode.USER_TAG_NOT_FOUND);
        }
        return assemble(boards);
    }

    public boolean isWriter(String boardId, Long userId) {
        return boardRepository.existsByBoardIdAndUserEntity_UserIdAndStatus(
                boardId, userId, BoardStatus.POST
        );
    }

    private BoardPage toPage(Page<BoardEntity> page) {
        return new BoardPage(assemble(page.getContent()), page.getTotalElements());
    }

    private List<BoardView> assemble(List<BoardEntity> boards) {
        if (boards.isEmpty()) {
            return List.of();
        }

        Map<String, List<String>> userTags = loadUserTags(boards);
        Map<SubjectRef, List<BoardSystemTag>> systemTags = loadSystemTags(boards);

        return boards.stream().map(board -> {
            BoardSubject subject = subjectOf(board);
            return new BoardView(
                    board.getBoardId(),
                    board.getContent(),
                    board.getCreatedAt(),
                    board.getUserEntity().getNickname(),
                    board.getLikeCount(),
                    subject,
                    userTags.getOrDefault(board.getBoardId(), List.of()),
                    systemTags.getOrDefault(new SubjectRef(subject.type(), subject.catalogId()), List.of())
            );
        }).toList();
    }

    private Map<String, List<String>> loadUserTags(List<BoardEntity> boards) {
        List<String> boardIds = boards.stream().map(BoardEntity::getBoardId).toList();
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (BoardUserTagNameRow row : boardUserTagRepository.findNamesByBoardIds(boardIds)) {
            result.computeIfAbsent(row.boardId(), ignored -> new ArrayList<>()).add(row.name());
        }
        return result;
    }

    private Map<SubjectRef, List<BoardSystemTag>> loadSystemTags(List<BoardEntity> boards) {
        Set<Long> trackIds = new LinkedHashSet<>();
        Set<Long> albumIds = new LinkedHashSet<>();
        for (BoardEntity board : boards) {
            if (board.getTrack() != null) {
                trackIds.add(board.getTrack().getTrackId());
            } else {
                albumIds.add(board.getAlbum().getAlbumId());
            }
        }

        Map<SubjectRef, List<BoardSystemTag>> result = new LinkedHashMap<>();
        appendResolved(result, SubjectType.TRACK, trackIds);
        appendResolved(result, SubjectType.ALBUM, albumIds);
        return result;
    }

    private void appendResolved(
            Map<SubjectRef, List<BoardSystemTag>> target,
            SubjectType type,
            Collection<Long> subjectIds
    ) {
        if (subjectIds.isEmpty()) {
            return;
        }
        List<SubjectTagResolvedEntity> rows = resolvedRepository.findVisibleBySubjectsWithTag(
                type, subjectIds, ResolvedStatus.HIDDEN
        );
        for (SubjectTagResolvedEntity row : rows) {
            SubjectRef subject = row.subjectRef();
            target.computeIfAbsent(subject, ignored -> new ArrayList<>()).add(new BoardSystemTag(
                    row.getTag().getTagId(), row.getTag().getName(), row.getScore()
            ));
        }
    }

    private BoardSubject subjectOf(BoardEntity board) {
        if (board.getTrack() != null && board.getAlbum() == null) {
            return BoardSubject.track(board.getTrack().getTrackId(), board.getTrack().getSpotifyId());
        }
        if (board.getAlbum() != null && board.getTrack() == null) {
            return BoardSubject.album(board.getAlbum().getAlbumId(), board.getAlbum().getSpotifyId());
        }
        throw new IllegalStateException("Board must target exactly one Track or Album: " + board.getBoardId());
    }

    private BoardEntity getPublished(String boardId) {
        return boardRepository.findByBoardIdAndStatus(boardId, BoardStatus.POST)
                .orElseThrow(() -> new CustomException(ExceptionCode.BOARD_NOT_FOUND));
    }
}

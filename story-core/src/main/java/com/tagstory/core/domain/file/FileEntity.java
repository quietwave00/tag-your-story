package com.tagstory.core.domain.file;

import com.tagstory.core.domain.board.BoardEntity;
import com.tagstory.core.domain.file.service.File;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "files")
public class FileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    private String fileName;

    private String filePath;

    @Column(columnDefinition = "VARCHAR(255)")
    @Enumerated(EnumType.STRING)
    private FileLevel fileLevel;

    @Column(columnDefinition = "VARCHAR(255)")
    @Enumerated(EnumType.STRING)
    private FileStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private BoardEntity board;


    /*
     * 연관관계 설정
     */
    public void addBoard(BoardEntity boardEntity) {
        this.board = boardEntity;
    }

    /*
     * 비즈니스 로직
     */
    public void delete() {
        this.status = FileStatus.PENDING;
    }

    public void setFileLevelToMain() {
        this.fileLevel = FileLevel.MAIN;
    }

    /*
     * 형변환
     */
    public File toFile() {
        return File.builder()
                .fileId(this.getFileId())
                .fileName(this.getFileName())
                .filePath(this.getFilePath())
                .fileLevel(this.getFileLevel())
                .status(this.getStatus())
                .board(this.getBoard().toBoard())
                .build();
    }
}

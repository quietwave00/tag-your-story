package com.tagnote.application.board.model;

import java.util.List;

public record BoardPage(List<BoardView> boards, long totalCount) {
    public BoardPage {
        boards = List.copyOf(boards);
    }
}

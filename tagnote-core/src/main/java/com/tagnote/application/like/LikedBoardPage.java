package com.tagnote.application.like;

import java.util.List;

public record LikedBoardPage(List<LikedBoardItem> items, long totalCount) {
}

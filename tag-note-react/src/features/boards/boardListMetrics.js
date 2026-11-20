import { boardService } from '../../services/boardService.js';
import { commentService } from '../../services/commentService.js';

export async function getBoardListMetrics(boards) {
  const entries = await Promise.all(boards.map(async ({ boardId }) => {
    const [detailResult, commentResult] = await Promise.allSettled([
      boardService.getBoardByBoardId(boardId),
      commentService.getCommentCountByBoardId(boardId),
    ]);

    return [boardId, {
      likeCount: detailResult.status === 'fulfilled' && Number.isInteger(detailResult.value?.likeCount)
        ? detailResult.value.likeCount : null,
      commentCount: commentResult.status === 'fulfilled' && Number.isInteger(commentResult.value?.count)
        ? commentResult.value.count : null,
    }];
  }));

  return Object.fromEntries(entries);
}

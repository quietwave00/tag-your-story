import { afterEach, describe, expect, it, vi } from 'vitest';
import { getBoardListMetrics } from './boardListMetrics.js';
import { boardService } from '../../services/boardService.js';
import { commentService } from '../../services/commentService.js';

afterEach(() => vi.restoreAllMocks());

describe('board list metrics', () => {
  it('keeps a successful count when the other request fails', async () => {
    vi.spyOn(boardService, 'getBoardByBoardId').mockRejectedValue(new Error('detail unavailable'));
    vi.spyOn(commentService, 'getCommentCountByBoardId').mockResolvedValue({ count: 4 });

    expect(await getBoardListMetrics([{ boardId: 'board-1' }])).toEqual({
      'board-1': { likeCount: null, commentCount: 4 },
    });
  });
});

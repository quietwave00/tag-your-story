import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/index.js';
import { ApiError, AuthExpiredError } from '../../services/apiClient.js';
import { commentService } from '../../services/commentService.js';
import CommentSection from './CommentSection.jsx';

const commentList = [{ comment: { commentId: 1, nickname: 'Listener', content: 'Original comment' }, children: [] }];

beforeEach(async () => {
  await i18n.changeLanguage('en');
  vi.spyOn(commentService, 'getCommentList').mockResolvedValue(commentList);
  vi.spyOn(commentService, 'getCommentCountByBoardId').mockResolvedValue({ count: 1 });
  vi.spyOn(commentService, 'getUserCommentId').mockResolvedValue([]);
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe('comment mutation entry', () => {
  it('keeps public comments readable and requests login before guest comment or reply writes', async () => {
    const onLoginRequired = vi.fn();
    const writeComment = vi.spyOn(commentService, 'writeComment');
    const writeReply = vi.spyOn(commentService, 'writeReply');
    render(<CommentSection boardId="board-1" isAuthenticated={false} onLoginRequired={onLoginRequired} />);

    expect(await screen.findByText('Original comment')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Reply' }));
    expect(onLoginRequired).toHaveBeenCalledWith(false);
    expect(screen.queryByPlaceholderText('Write a reply')).not.toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText('Write a comment'), { target: { value: 'My draft' } });
    fireEvent.click(screen.getAllByRole('button', { name: 'WRITE' }).at(-1));
    expect(onLoginRequired).toHaveBeenCalledWith(true);
    expect(writeComment).not.toHaveBeenCalled();
    expect(writeReply).not.toHaveBeenCalled();
  });

  it('keeps the authenticated comment request', async () => {
    const onLoginRequired = vi.fn();
    const writeComment = vi.spyOn(commentService, 'writeComment').mockResolvedValue({});
    render(<CommentSection boardId="board-1" isAuthenticated onLoginRequired={onLoginRequired} />);

    fireEvent.change(screen.getByPlaceholderText('Write a comment'), { target: { value: 'New comment' } });
    fireEvent.click(screen.getByRole('button', { name: 'WRITE' }));
    await waitFor(() => expect(writeComment).toHaveBeenCalledWith('board-1', 'New comment'));
    expect(onLoginRequired).not.toHaveBeenCalled();
  });

  it('keeps the authenticated reply request', async () => {
    const onLoginRequired = vi.fn();
    const writeReply = vi.spyOn(commentService, 'writeReply').mockResolvedValue({});
    render(<CommentSection boardId="board-1" isAuthenticated onLoginRequired={onLoginRequired} />);

    fireEvent.click(await screen.findByRole('button', { name: 'Reply' }));
    fireEvent.change(screen.getByPlaceholderText('Write a reply'), { target: { value: 'New reply' } });
    fireEvent.click(screen.getAllByRole('button', { name: 'WRITE' }).at(-1));
    await waitFor(() => expect(writeReply).toHaveBeenCalledWith('board-1', 1, 'New reply'));
    expect(onLoginRequired).not.toHaveBeenCalled();
  });

  it('routes only an expired comment mutation to login', async () => {
    const onLoginRequired = vi.fn();
    const writeComment = vi.spyOn(commentService, 'writeComment')
      .mockRejectedValueOnce(new ApiError('Denied', { exceptionCode: 'NO_PERMISSION' }))
      .mockRejectedValueOnce(new AuthExpiredError());
    render(<CommentSection boardId="board-1" isAuthenticated onLoginRequired={onLoginRequired} />);

    expect(await screen.findByText('Original comment')).toBeInTheDocument();
    fireEvent.change(screen.getByPlaceholderText('Write a comment'), { target: { value: 'Draft' } });
    fireEvent.click(screen.getByRole('button', { name: 'WRITE' }));
    await waitFor(() => expect(writeComment).toHaveBeenCalledTimes(1));
    expect(await screen.findByText('We could not process the comment request.')).toBeInTheDocument();
    expect(onLoginRequired).not.toHaveBeenCalled();

    fireEvent.click(screen.getByRole('button', { name: 'WRITE' }));
    await waitFor(() => expect(onLoginRequired).toHaveBeenCalledWith(true, 'expired'));
  });

  it('keeps a mutation error visible when the initial public list finishes afterward', async () => {
    let finishList;
    vi.mocked(commentService.getCommentList).mockImplementationOnce(() => new Promise((resolve) => { finishList = resolve; }));
    vi.spyOn(commentService, 'writeComment').mockRejectedValue(new ApiError('Denied', { exceptionCode: 'NO_PERMISSION' }));
    render(<CommentSection boardId="board-1" isAuthenticated onLoginRequired={vi.fn()} />);

    fireEvent.change(screen.getByPlaceholderText('Write a comment'), { target: { value: 'Draft' } });
    fireEvent.click(screen.getByRole('button', { name: 'WRITE' }));
    expect(await screen.findByText('We could not process the comment request.')).toBeInTheDocument();

    await act(async () => finishList(commentList));
    expect(await screen.findByText('Original comment')).toBeInTheDocument();
    expect(screen.getByText('We could not process the comment request.')).toBeInTheDocument();
  });
});
